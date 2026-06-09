package com.springbank.loan.service;

import com.springbank.common.enums.CreditGrade;
import com.springbank.loan.entity.CreditScore;
import com.springbank.loan.repository.CreditScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * CREDIT SCORE SERVICE — اعتبارسنجی و به‌روزرسانی امتیاز اعتباری
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoreService {

    private final CreditScoreRepository creditScoreRepository;

    /**
     * واحد درآمد پایه برای محاسبه‌ی سقف وام.
     * حداکثر وام = getLoanMultiplier × این واحد (ساده‌سازی، چون درآمد ماهانه‌ی واقعی در مدل نیست).
     */
    @Value("${app.loan.base-income-unit:10000000}")
    private BigDecimal baseIncomeUnit;

    @Transactional(readOnly = true)
    public BigDecimal getRecommendedInterestRate(Long userId) {
        return creditScoreRepository.findByUserId(userId)
                .map(CreditScore::getRecommendedInterestRate)
                .orElse(new BigDecimal("18.0")); // نرخ پیش‌فرض
    }

    @Transactional(readOnly = true)
    public CreditScore getCreditScore(Long userId) {
        return creditScoreRepository.findByUserId(userId).orElse(null);
    }

    /**
     * حداکثر مبلغ وام مجاز بر اساس امتیاز اعتباری (getLoanMultiplier × واحد درآمد پایه).
     * اگر کاربر credit score نداشته باشد، یک سقف محافظه‌کارانه برمی‌گردد.
     */
    @Transactional(readOnly = true)
    public BigDecimal getMaxAllowedLoanAmount(Long userId) {
        CreditScore cs = creditScoreRepository.findByUserId(userId).orElse(null);
        double multiplier = cs != null ? cs.getLoanMultiplier() : 12.0; // معادل POOR
        return baseIncomeUnit.multiply(BigDecimal.valueOf(multiplier)).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * بهبود امتیاز اعتباری پس از پرداخت قسط (فلوی ۱۰).
     * پرداخت به‌موقع → افزایش امتیاز و فاکتور PaymentHistory؛ پرداخت با تأخیر → کاهش جزئی.
     *
     * @param userId    شناسه‌ی کاربر
     * @param onTime    آیا قسط به‌موقع پرداخت شد؟
     */
    @Transactional
    public void recordInstallmentPayment(Long userId, boolean onTime) {
        creditScoreRepository.findByUserId(userId).ifPresent(cs -> {
            int delta = onTime ? 5 : -10;
            int newScore = clamp(cs.getScore() + delta, 300, 850);
            cs.setScore(newScore);
            cs.setGrade(CreditScore.determineGrade(newScore));

            // فاکتور PaymentHistory (۰ تا ۱۰۰)
            BigDecimal ph = cs.getPaymentHistoryScore() == null ? new BigDecimal("50") : cs.getPaymentHistoryScore();
            BigDecimal phDelta = onTime ? new BigDecimal("2") : new BigDecimal("-5");
            cs.setPaymentHistoryScore(clampBd(ph.add(phDelta), BigDecimal.ZERO, new BigDecimal("100")));

            cs.setCalculatedAt(LocalDateTime.now());
            cs.setNextRecalculationAt(LocalDateTime.now().plusDays(30));
            creditScoreRepository.save(cs);

            log.info("[CREDIT] امتیاز اعتباری userId={} به‌روزرسانی شد: score={}, grade={}, onTime={}",
                    userId, newScore, cs.getGrade(), onTime);
        });
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private BigDecimal clampBd(BigDecimal v, BigDecimal min, BigDecimal max) {
        if (v.compareTo(min) < 0) return min;
        if (v.compareTo(max) > 0) return max;
        return v;
    }
}
