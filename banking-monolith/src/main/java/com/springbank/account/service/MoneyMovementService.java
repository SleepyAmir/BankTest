package com.springbank.account.service;

import com.springbank.account.dto.DepositRequestDto;
import com.springbank.account.dto.TransactionResultDto;
import com.springbank.account.dto.TransferRequestDto;
import com.springbank.account.entity.Account;
import com.springbank.account.repository.AccountRepository;
import com.springbank.common.annotation.Auditable;
import com.springbank.common.enums.NotificationType;
import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.common.exception.BusinessException;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.notification.service.NotificationService;
import com.springbank.user.entity.KycVerification;
import com.springbank.user.repository.KycVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ============================================================================
 * MONEY MOVEMENT SERVICE — شارژ و انتقال وجه «اتمیک» (فلوهای ۴ و ۵)
 * ============================================================================
 * تصمیم معماری: جابجایی پول به‌صورت اتمیک «داخل monolith» و در یک @Transactional
 * انجام می‌شود؛ سپس یک TransactionCompletedEvent به RabbitMQ منتشر می‌شود تا
 * میکروسرویس‌ها (transaction-read, fraud, analytics, audit, notification) رکورد
 * تراکنش را بسازند/پردازش کنند.
 *
 * این رویکرد، مشکل بحرانی «اتمیک نبودن انتقال» در نسخه‌ی قبلی (دو فراخوانی REST جدا)
 * را برطرف می‌کند: یا هر دو طرف انتقال انجام می‌شود یا هیچ‌کدام (rollback).
 *
 * کنترل سقف: سقف مؤثر = min(سقف حساب، سقف KYC) — هم روزانه و هم ماهانه.
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MoneyMovementService {

    private final AccountRepository accountRepository;
    private final KycVerificationRepository kycRepository;
    private final NotificationService notificationService;
    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE = "banking.exchange";
    public static final String ROUTING_TRANSACTION_COMPLETED = "transaction.completed";

    /**
     * شارژ حساب (DEPOSIT) — موجودی افزایش می‌یابد، وضعیت COMPLETED و کد پیگیری یکتا ساخته می‌شود.
     */
    @Transactional
    @Auditable(action = "DEPOSIT", entity = "Account")
    public TransactionResultDto deposit(DepositRequestDto dto) {
        log.info("[DEPOSIT] شارژ حساب id={} به مبلغ {}", dto.accountId(), dto.amount());

        Account account = loadActiveAccount(dto.accountId());
        if (!account.isActive()) {
            throw new BusinessException("حساب فعال نیست و امکان شارژ وجود ندارد");
        }

        account.deposit(dto.amount()); // اعتبارسنجی مبلغ مثبت در خود entity
        accountRepository.save(account);

        String trackingCode = generateTrackingCode();
        String category = dto.spendingCategory() != null ? dto.spendingCategory() : "salary";

        log.info("[DEPOSIT] ✅ حساب id={} شارژ شد. موجودی جدید={}", account.getId(), account.getBalance());

        publishTransactionEvent(trackingCode, "DEPOSIT", null, account.getId(),
                account.getUser().getId(), dto.amount(), category);

        notificationService.notifyInApp(account.getUser(), NotificationType.TRANSACTION_DONE,
                "شارژ حساب موفق",
                String.format("مبلغ %s به حساب شما واریز شد. کد پیگیری: %s", dto.amount(), trackingCode),
                null);

        return new TransactionResultDto(trackingCode, "DEPOSIT", "COMPLETED", dto.amount(),
                null, account.getId(), null, account.getBalance());
    }

    /**
     * انتقال وجه داخلی اتمیک بین دو حساب (فلوی ۵).
     * <p>
     * بررسی‌ها (به ترتیب):
     *  ۱. حساب مبدأ و مقصد متفاوت باشند و هر دو فعال باشند.
     *  ۲. موجودی مبدأ کافی باشد.
     *  ۳. سقف روزانه و ماهانه‌ی مؤثر (min حساب و KYC) رعایت شود.
     * در صورت موفقیت: از مبدأ کم و به مقصد اضافه می‌شود، مصرف ثبت، event منتشر و
     * برای هر دو طرف نوتیفیکیشن ساخته می‌شود.
     */
    @Transactional
    @Auditable(action = "TRANSFER", entity = "Account")
    public TransactionResultDto transfer(TransferRequestDto dto) {
        log.info("[TRANSFER] انتقال {} از حساب {} به حساب {}",
                dto.amount(), dto.fromAccountId(), dto.toAccountId());

        if (dto.fromAccountId().equals(dto.toAccountId())) {
            throw new BusinessException("حساب مبدأ و مقصد نمی‌توانند یکسان باشند");
        }

        Account from = loadActiveAccount(dto.fromAccountId());
        Account to = loadActiveAccount(dto.toAccountId());

        // ۱. وضعیت حساب‌ها
        if (!from.isActive()) {
            throw new BusinessException("حساب مبدأ فعال نیست");
        }
        if (!to.isActive()) {
            throw new BusinessException("حساب مقصد فعال نیست");
        }

        // ۲. موجودی کافی
        if (from.getBalance().compareTo(dto.amount()) < 0) {
            throw new BusinessException("موجودی حساب مبدأ کافی نیست");
        }

        // ۳. سقف روزانه/ماهانه‌ی مؤثر = min(سقف حساب، سقف KYC)
        from.resetTransferCountersIfNeeded();
        BigDecimal effectiveDaily = effectiveDailyLimit(from);
        BigDecimal effectiveMonthly = effectiveMonthlyLimit(from);

        if (!from.isWithinDailyLimit(dto.amount(), effectiveDaily)) {
            throw new BusinessException(String.format(
                    "سقف انتقال روزانه رعایت نشده است. سقف: %s، مصرف امروز: %s",
                    effectiveDaily, from.getDailyTransferred()));
        }
        if (!from.isWithinMonthlyLimit(dto.amount(), effectiveMonthly)) {
            throw new BusinessException(String.format(
                    "سقف انتقال ماهانه رعایت نشده است. سقف: %s، مصرف این ماه: %s",
                    effectiveMonthly, from.getMonthlyTransferred()));
        }

        // ===== اجرای اتمیک =====
        from.withdraw(dto.amount());
        to.deposit(dto.amount());
        from.registerOutgoingTransfer(dto.amount());

        accountRepository.save(from);
        accountRepository.save(to);

        String trackingCode = generateTrackingCode();
        log.info("[TRANSFER] ✅ انتقال موفق. trackingCode={}, موجودی مبدأ={}, موجودی مقصد={}",
                trackingCode, from.getBalance(), to.getBalance());

        publishTransactionEvent(trackingCode, "TRANSFER", from.getId(), to.getId(),
                from.getUser().getId(), dto.amount(),
                dto.spendingCategory() != null ? dto.spendingCategory() : "transfer");

        // نوتیفیکیشن برای مبدأ و مقصد
        notificationService.notifyInApp(from.getUser(), NotificationType.TRANSACTION_DONE,
                "انتقال وجه موفق",
                String.format("مبلغ %s از حساب شما منتقل شد. کد پیگیری: %s", dto.amount(), trackingCode),
                null);
        notificationService.notifyInApp(to.getUser(), NotificationType.TRANSACTION_DONE,
                "دریافت وجه",
                String.format("مبلغ %s به حساب شما واریز شد. کد پیگیری: %s", dto.amount(), trackingCode),
                null);

        return new TransactionResultDto(trackingCode, "TRANSFER", "COMPLETED", dto.amount(),
                from.getId(), to.getId(), from.getBalance(), to.getBalance());
    }

    // ===================== Helpers =====================

    private Account loadActiveAccount(Long id) {
        return accountRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }

    private BigDecimal effectiveDailyLimit(Account account) {
        BigDecimal accountLimit = account.getDailyTransferLimit();
        BigDecimal kycLimit = kycRepository.findByUserId(account.getUser().getId())
                .map(KycVerification::getDailyTransferLimit)
                .orElse(accountLimit);
        return accountLimit.min(kycLimit);
    }

    private BigDecimal effectiveMonthlyLimit(Account account) {
        BigDecimal accountLimit = account.getMonthlyTransferLimit();
        BigDecimal kycLimit = kycRepository.findByUserId(account.getUser().getId())
                .map(KycVerification::getMonthlyTransferLimit)
                .orElse(accountLimit);
        return accountLimit.min(kycLimit);
    }

    private String generateTrackingCode() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    /**
     * انتشار رویداد تکمیل تراکنش به RabbitMQ.
     * در صورت خطا، تراکنش پولی (که قبلاً commit می‌شود) باقی می‌ماند و فقط لاگ هشدار ثبت می‌شود.
     * (در فاز بعد می‌توان الگوی Transactional Outbox را برای تضمین تحویل اضافه کرد.)
     */
    private void publishTransactionEvent(String trackingCode, String type, Long fromId, Long toId,
                                         Long userId, BigDecimal amount, String category) {
        try {
            TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                    .trackingCode(trackingCode)
                    .fromAccountId(fromId)
                    .toAccountId(toId)
                    .userId(userId)
                    .amount(amount)
                    .type(type)
                    .status("COMPLETED")
                    .spendingCategory(category)
                    .timestamp(LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_TRANSACTION_COMPLETED, event);
            log.info("[TX-EVENT] ✅ TransactionCompletedEvent منتشر شد (trackingCode={})", trackingCode);
        } catch (Exception e) {
            log.warn("[TX-EVENT] ⚠️ انتشار رویداد ناموفق بود؛ پول جابجا شد ولی میکروسرویس‌ها مطلع نشدند: {}",
                    e.getMessage());
        }
    }
}
