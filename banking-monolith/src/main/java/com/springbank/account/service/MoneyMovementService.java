package com.springbank.account.service;

import com.springbank.account.dto.InternalTransferDto;
import com.springbank.account.dto.TransactionResultDto;
import com.springbank.account.entity.Account;
import com.springbank.account.repository.AccountRepository;
import com.springbank.common.exception.BusinessException;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.user.entity.KycVerification;
import com.springbank.user.repository.KycVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * ============================================================================
 * MONEY MOVEMENT SERVICE — جابجایی «اتمیک» پول (سرویس‌به‌سرویس)
 * ============================================================================
 * این سرویس صرفاً مسئول جابجایی اتمیک پول در DB است و توسط endpointهای /internal
 * (که از transaction-write فراخوانی می‌شوند) استفاده می‌شود. هیچ event یا
 * notification منتشر نمی‌کند — آن مسئولیت با transaction-write است (تنها مرجع تراکنش).
 *
 * اتمیک بودن: هر متد در یک @Transactional اجرا می‌شود؛ در صورت هر خطا، کل تغییرات
 * (برداشت و واریز) rollback می‌شوند → پول گم نمی‌شود.
 *
 * کنترل سقف: سقف مؤثر = min(سقف حساب، سقف KYC) — وقتی enforceLimits=true.
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MoneyMovementService {

    private final AccountRepository accountRepository;
    private final KycVerificationRepository kycRepository;

    /**
     * انتقال وجه اتمیک بین دو حساب (در یک تراکنش DB).
     */
    @Transactional
    public TransactionResultDto transferAtomic(InternalTransferDto dto) {
        log.info("[TX-ATOMIC] انتقال اتمیک {} از {} به {}", dto.amount(), dto.fromAccountId(), dto.toAccountId());

        if (dto.fromAccountId().equals(dto.toAccountId())) {
            throw new BusinessException("حساب مبدأ و مقصد نمی‌توانند یکسان باشند");
        }

        BigDecimal fee = dto.fee() != null ? dto.fee() : BigDecimal.ZERO;
        BigDecimal totalDeduction = dto.amount().add(fee);

        Account from = loadActiveAccount(dto.fromAccountId());
        Account to = loadActiveAccount(dto.toAccountId());

        if (!from.isActive()) throw new BusinessException("حساب مبدأ فعال نیست");
        if (!to.isActive()) throw new BusinessException("حساب مقصد فعال نیست");
        if (from.getBalance().compareTo(totalDeduction) < 0) {
            throw new BusinessException("موجودی حساب مبدأ کافی نیست (با احتساب کارمزد)");
        }

        if (dto.enforceLimits()) {
            from.resetTransferCountersIfNeeded();
            BigDecimal effDaily = effectiveDailyLimit(from);
            BigDecimal effMonthly = effectiveMonthlyLimit(from);
            if (!from.isWithinDailyLimit(totalDeduction, effDaily)) {
                throw new BusinessException("سقف انتقال روزانه رعایت نشده است. سقف: " + effDaily);
            }
            if (!from.isWithinMonthlyLimit(totalDeduction, effMonthly)) {
                throw new BusinessException("سقف انتقال ماهانه رعایت نشده است. سقف: " + effMonthly);
            }
        }

        from.withdraw(totalDeduction);
        to.deposit(dto.amount());
        from.registerOutgoingTransfer(totalDeduction);

        accountRepository.save(from);
        accountRepository.save(to);

        log.info("[TX-ATOMIC] ✅ انتقال اتمیک انجام شد. موجودی مبدأ={}, مقصد={}",
                from.getBalance(), to.getBalance());
        return new TransactionResultDto(null, "TRANSFER", "COMPLETED", dto.amount(),
                from.getId(), to.getId(), from.getBalance(), to.getBalance());
    }

    /** واریز اتمیک (DEPOSIT/REFUND/LOAN_DISBURSEMENT). */
    @Transactional
    public TransactionResultDto depositAtomic(Long accountId, BigDecimal amount) {
        Account account = loadActiveAccount(accountId);
        if (!account.isActive()) throw new BusinessException("حساب فعال نیست");
        account.deposit(amount);
        accountRepository.save(account);
        log.info("[TX-ATOMIC] ✅ واریز اتمیک {} به حساب {}. موجودی={}", amount, accountId, account.getBalance());
        return new TransactionResultDto(null, "DEPOSIT", "COMPLETED", amount,
                null, account.getId(), null, account.getBalance());
    }

    /** برداشت اتمیک (WITHDRAWAL/CARD_PAYMENT/LOAN_PAYMENT). */
    @Transactional
    public TransactionResultDto withdrawAtomic(Long accountId, BigDecimal amount) {
        Account account = loadActiveAccount(accountId);
        if (!account.isActive()) throw new BusinessException("حساب فعال نیست");
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("موجودی حساب کافی نیست");
        }
        account.withdraw(amount);
        accountRepository.save(account);
        log.info("[TX-ATOMIC] ✅ برداشت اتمیک {} از حساب {}. موجودی={}", amount, accountId, account.getBalance());
        return new TransactionResultDto(null, "WITHDRAWAL", "COMPLETED", amount,
                account.getId(), null, account.getBalance(), null);
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
}
