package com.springbank.fraud.service;

import com.springbank.common.enums.AlertSeverity;
import com.springbank.common.enums.AlertStatus;
import com.springbank.common.enums.AmlAlertType;
import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.fraud.entity.AmlAlert;
import com.springbank.fraud.entity.TransactionHistory;
import com.springbank.fraud.repository.AmlAlertRepository;
import com.springbank.fraud.repository.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * ============================================================================
 * AML ANALYSIS SERVICE — بررسی ضدپولشویی (فلوی ۷)
 * ============================================================================
 * همزمان با تکمیل تراکنش، چک‌های زیر اجرا می‌شوند و در صورت برخورد، AmlAlert ساخته
 * می‌شود (لینک‌شده به user و transaction، قابل مشاهده در پنل گزارش‌های مشکوک):
 *
 *   - LARGE_TRANSACTION : مبلغ بیش از ۵۰۰ میلیون → severity MEDIUM
 *   - ROUND_AMOUNT      : مبالغ گرد متوالی (مثلاً چند ۱۰٬۰۰۰٬۰۰۰ پشت سر هم)
 *   - STRUCTURING       : تقسیم یک مبلغ بزرگ به چند تراکنش کوچک‌تر در بازه‌ی کوتاه
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmlAnalysisService {

    private final AmlAlertRepository amlAlertRepository;
    private final TransactionHistoryRepository historyRepository;

    /** آستانه‌ی تراکنش بزرگ برای AML: ۵۰۰ میلیون. */
    private static final BigDecimal LARGE_TX_THRESHOLD = new BigDecimal("500000000");
    /** واحد مبلغ گرد (۱۰ میلیون). */
    private static final BigDecimal ROUND_UNIT = new BigDecimal("10000000");
    /** آستانه‌ی جمع Structuring (نزدیک سقف گزارش‌دهی). */
    private static final BigDecimal STRUCTURING_SUM_THRESHOLD = new BigDecimal("450000000");

    @Transactional
    public void analyze(TransactionCompletedEvent event) {
        log.info("[AML] بررسی ضدپولشویی برای trackingCode={}, amount={}", event.getTrackingCode(), event.getAmount());

        // قانون ۱: تراکنش بزرگ (> 500M)
        if (event.getAmount().compareTo(LARGE_TX_THRESHOLD) > 0) {
            createAlert(event, AmlAlertType.LARGE_TRANSACTION, AlertSeverity.MEDIUM,
                    "تراکنش بزرگ بیش از ۵۰۰ میلیون: " + event.getAmount());
        }

        // قانون ۲: مبلغ گرد متوالی
        if (isRoundAmount(event.getAmount()) && hasConsecutiveRoundAmounts(event)) {
            createAlert(event, AmlAlertType.ROUND_AMOUNT, AlertSeverity.MEDIUM,
                    "الگوی مبالغ گرد متوالی شناسایی شد (مضربی از ۱۰ میلیون)");
        }

        // قانون ۳: ساختاربندی (Structuring)
        if (detectStructuring(event)) {
            createAlert(event, AmlAlertType.STRUCTURING, AlertSeverity.HIGH,
                    "الگوی ساختاربندی: چند تراکنش کوچک‌تر با جمع نزدیک به سقف گزارش‌دهی");
        }
    }

    // ===================== Rules =====================

    private boolean isRoundAmount(BigDecimal amount) {
        return amount.remainder(ROUND_UNIT).compareTo(BigDecimal.ZERO) == 0;
    }

    /** آیا تراکنش‌های اخیر کاربر هم مبالغ گرد بوده‌اند؟ (حداقل ۳ مورد متوالی شامل تراکنش فعلی) */
    private boolean hasConsecutiveRoundAmounts(TransactionCompletedEvent event) {
        List<TransactionHistory> recent = historyRepository
                .findTop10ByUserIdOrderByOccurredAtDesc(event.getUserId());
        int consecutive = 1; // خود تراکنش فعلی
        for (TransactionHistory h : recent) {
            if (isRoundAmount(h.getAmount())) {
                consecutive++;
                if (consecutive >= 3) return true;
            } else {
                break; // فقط متوالی‌ها مهم‌اند
            }
        }
        return false;
    }

    /**
     * تشخیص Structuring: چند تراکنش «کوچک‌تر از آستانه‌ی تراکنش بزرگ» که جمع آن‌ها در یک
     * بازه‌ی کوتاه (۲۴ ساعت) از آستانه‌ی Structuring عبور کند — نشانه‌ی تقسیم عمدی مبلغ بزرگ.
     */
    private boolean detectStructuring(TransactionCompletedEvent event) {
        List<TransactionHistory> recent = historyRepository
                .findTop10ByUserIdOrderByOccurredAtDesc(event.getUserId());

        BigDecimal sum = event.getAmount();
        int smallCount = event.getAmount().compareTo(LARGE_TX_THRESHOLD) < 0 ? 1 : 0;

        java.time.LocalDateTime windowStart = java.time.LocalDateTime.now().minusHours(24);
        for (TransactionHistory h : recent) {
            if (h.getOccurredAt().isBefore(windowStart)) continue;
            if (h.getAmount().compareTo(LARGE_TX_THRESHOLD) < 0) {
                sum = sum.add(h.getAmount());
                smallCount++;
            }
        }
        // حداقل ۳ تراکنش کوچک با جمعی بالاتر از آستانه
        return smallCount >= 3 && sum.compareTo(STRUCTURING_SUM_THRESHOLD) >= 0;
    }

    // ===================== Persistence =====================

    private void createAlert(TransactionCompletedEvent event, AmlAlertType type, AlertSeverity severity, String description) {
        AmlAlert alert = AmlAlert.builder()
                .type(type)
                .severity(severity)
                .status(AlertStatus.OPEN)
                .description(description)
                .userId(event.getUserId())
                .transactionId(event.getTransactionId())
                .build();
        amlAlertRepository.save(alert);
        log.warn("[AML] 🚨 AmlAlert ثبت شد: type={}, severity={}, userId={}, txId={}",
                type, severity, event.getUserId(), event.getTransactionId());
    }
}
