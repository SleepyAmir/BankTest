package com.springbank.transaction.write.service;

import com.springbank.common.enums.TransactionStatus;
import com.springbank.common.enums.TransactionType;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.transaction.write.dto.response.TransactionResponseDto;
import com.springbank.transaction.write.dto.request.TransactionCreateDto;
import com.springbank.transaction.write.entity.Transaction;
import com.springbank.transaction.write.mapper.TransactionMapper;
import com.springbank.transaction.write.messaging.TransactionEventPublisher;
import com.springbank.transaction.write.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ============================================================================
 * TRANSACTION WRITE SERVICE — CQRS Write Model (نسخه‌ی اتمیک)
 * ============================================================================
 * جریان جدید (اتمیک):
 *   createTransaction → یک فراخوانی «اتمیک» به monolith برای جابجایی پول →
 *   در صورت موفقیت: status=COMPLETED، ذخیره، انتشار event برای read/fraud/analytics/notification.
 *   در صورت خطا: status=FAILED ذخیره می‌شود و پول جابجا نشده (rollback در سمت monolith).
 *
 * این نسخه مشکل بحرانی نسخه‌ی قبلی (دو فراخوانی REST جدا برای withdraw و deposit
 * که می‌توانست پول را گم کند) را با یک endpoint اتمیک در monolith حل می‌کند.
 *
 * LOG MARKERS: [TX-CREATE] [TX-MOVE] [TX-SAVE] [TX-PUBLISH] [TX-FAIL] [TX-REVERSE]
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionWriteService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionEventPublisher eventPublisher;
    private final RestTemplate restTemplate;

    @Value("${services.monolith.url:http://localhost:8081}")
    private String monolithBaseUrl;

    /**
     * ایجاد و «تکمیل اتمیک» یک تراکنش.
     */
    @Transactional
    public TransactionResponseDto createTransaction(TransactionCreateDto dto) {
        log.info("[TX-CREATE] تراکنش جدید: type={}, amount={}, from={}, to={}",
                dto.type(), dto.amount(), dto.fromAccountId(), dto.toAccountId());

        validate(dto);

        Transaction tx = transactionMapper.toEntity(dto);
        tx.setTrackingCode(generateTrackingCode());
        tx.setCurrency(dto.currency() != null ? dto.currency() : "IRR");
        tx.setStatus(TransactionStatus.PENDING);

        try {
            // جابجایی اتمیک پول در monolith (یک فراخوانی)
            moveMoneyAtomically(dto);

            tx.setStatus(TransactionStatus.COMPLETED);
            Transaction saved = transactionRepository.save(tx);
            log.info("[TX-SAVE] ✅ تراکنش COMPLETED ذخیره شد: trackingCode={}", saved.getTrackingCode());

            // انتشار رویداد برای سایر سرویس‌ها (read/fraud/analytics/notification)
            try {
                eventPublisher.publishTransactionCompleted(saved);
                log.info("[TX-PUBLISH] ✅ رویداد منتشر شد");
            } catch (Exception e) {
                log.warn("[TX-PUBLISH] ⚠️ انتشار رویداد ناموفق بود (تراکنش انجام شده): {}", e.getMessage());
            }
            return transactionMapper.toDto(saved);

        } catch (Exception e) {
            // پول جابجا نشده (rollback سمت monolith)؛ تراکنش را FAILED ثبت می‌کنیم.
            log.error("[TX-FAIL] ❌ جابجایی پول ناموفق بود: {}", e.getMessage());
            tx.setStatus(TransactionStatus.FAILED);
            tx.setDescription((tx.getDescription() == null ? "" : tx.getDescription()) + " | Failed: " + e.getMessage());
            transactionRepository.save(tx);
            throw new IllegalStateException("تراکنش ناموفق بود: " + e.getMessage());
        }
    }

    /**
     * معکوس کردن یک تراکنش COMPLETED (اتمیک).
     */
    @Transactional
    public TransactionResponseDto reverseTransaction(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));

        if (tx.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("فقط تراکنش‌های COMPLETED قابل برگشت هستند. وضعیت: " + tx.getStatus());
        }

        // برگشت: انتقال معکوس (اتمیک)
        if (tx.getType() == TransactionType.TRANSFER && tx.getFromAccountId() != null && tx.getToAccountId() != null) {
            callTransfer(tx.getToAccountId(), tx.getFromAccountId(), tx.getAmount(), false);
        } else if (tx.getFromAccountId() != null) {
            callDeposit(tx.getFromAccountId(), tx.getAmount()); // برگشت برداشت
        } else if (tx.getToAccountId() != null) {
            callWithdraw(tx.getToAccountId(), tx.getAmount()); // برگشت واریز
        }

        tx.setStatus(TransactionStatus.REVERSED);
        Transaction saved = transactionRepository.save(tx);
        eventPublisher.publishTransactionCompleted(saved);
        log.info("[TX-REVERSE] ✅ تراکنش id={} برگشت داده شد", saved.getId());
        return transactionMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponseDto getById(Long id) {
        return transactionRepository.findById(id)
                .map(transactionMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    // ===================== Internal =====================

    private void validate(TransactionCreateDto dto) {
        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("مبلغ باید بزرگ‌تر از صفر باشد");
        }
        if (dto.type() == null) {
            throw new IllegalArgumentException("نوع تراکنش الزامی است");
        }
    }

    private void moveMoneyAtomically(TransactionCreateDto dto) {
        switch (dto.type()) {
            case TRANSFER -> {
                requireAccounts(dto.fromAccountId(), dto.toAccountId());
                callTransfer(dto.fromAccountId(), dto.toAccountId(), dto.amount(), true);
            }
            case WITHDRAWAL, CARD_PAYMENT -> {
                requireAccount(dto.fromAccountId(), "حساب مبدأ");
                callWithdraw(dto.fromAccountId(), dto.amount());
            }
            case DEPOSIT, REFUND, LOAN_DISBURSEMENT -> {
                requireAccount(dto.toAccountId(), "حساب مقصد");
                callDeposit(dto.toAccountId(), dto.amount());
            }
            case LOAN_PAYMENT -> {
                requireAccount(dto.fromAccountId(), "حساب مبدأ");
                callWithdraw(dto.fromAccountId(), dto.amount());
            }
            default -> throw new IllegalArgumentException("نوع تراکنش پشتیبانی نمی‌شود: " + dto.type());
        }
    }

    private void requireAccounts(Long from, Long to) {
        requireAccount(from, "حساب مبدأ");
        requireAccount(to, "حساب مقصد");
    }

    private void requireAccount(Long id, String name) {
        if (id == null) throw new IllegalArgumentException(name + " الزامی است");
    }

    /** فراخوانی انتقال اتمیک در monolith (یک تراکنش DB). */
    private void callTransfer(Long fromId, Long toId, BigDecimal amount, boolean enforceLimits) {
        String url = monolithBaseUrl + "/internal/accounts/transfer";
        Map<String, Object> body = new HashMap<>();
        body.put("fromAccountId", fromId);
        body.put("toAccountId", toId);
        body.put("amount", amount);
        body.put("enforceLimits", enforceLimits);
        restTemplate.postForEntity(url, body, String.class);
        log.info("[TX-MOVE] ✅ انتقال اتمیک monolith موفق ({} → {})", fromId, toId);
    }

    private void callDeposit(Long accountId, BigDecimal amount) {
        String url = monolithBaseUrl + "/internal/accounts/" + accountId + "/deposit?amount=" + amount;
        restTemplate.postForEntity(url, null, String.class);
        log.info("[TX-MOVE] ✅ واریز monolith موفق (account={})", accountId);
    }

    private void callWithdraw(Long accountId, BigDecimal amount) {
        String url = monolithBaseUrl + "/internal/accounts/" + accountId + "/withdraw?amount=" + amount;
        restTemplate.postForEntity(url, null, String.class);
        log.info("[TX-MOVE] ✅ برداشت monolith موفق (account={})", accountId);
    }

    private String generateTrackingCode() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
