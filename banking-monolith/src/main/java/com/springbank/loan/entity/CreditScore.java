package com.springbank.loan.entity;
import com.springbank.user.entity.User;
import com.springbank.common.entity.BaseEntity;

import com.springbank.common.enums.CreditGrade;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_scores")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CreditScore extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // امتیاز کلی (مثل FICO): 300 تا 850
    @Column(nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditGrade grade;
    // POOR(300-579), FAIR(580-669), GOOD(670-739)
    // VERY_GOOD(740-799), EXCEPTIONAL(800-850)

    // ========= فاکتورهای تشکیل‌دهنده امتیاز =========

    // ۳۵٪ - تاریخچه پرداخت اقساط (آیا به موقع پرداخت شده؟)
    @Column(name = "payment_history_score")
    private BigDecimal paymentHistoryScore;

    // ۳۰٪ - نسبت بدهی به اعتبار (هرچه کمتر، بهتر)
    @Column(name = "credit_utilization_score")
    private BigDecimal creditUtilizationScore;

    // ۱۵٪ - سن حساب (هرچه قدیمی‌تر، بهتر)
    @Column(name = "account_age_score")
    private BigDecimal accountAgeScore;

    // ۱۰٪ - تنوع در انواع اعتبار (وام، کارت اعتباری)
    @Column(name = "credit_mix_score")
    private BigDecimal creditMixScore;

    // ۱۰٪ - درخواست‌های اعتبار جدید (زیاد = منفی)
    @Column(name = "new_credit_score")
    private BigDecimal newCreditScore;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    // تاریخ محاسبه مجدد (هر ۳۰ روز یکبار)
    @Column(name = "next_recalculation_at")
    private LocalDateTime nextRecalculationAt;

    // ======== RELATIONS ========

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ======== HELPER METHODS ========

    // نرخ بهره پیشنهادی بر اساس امتیاز (کمتر = بهتر برای کاربر)
    public BigDecimal getRecommendedInterestRate() {
        return switch (grade) {
            case EXCEPTIONAL -> new BigDecimal("10.0");
            case VERY_GOOD   -> new BigDecimal("14.0");
            case GOOD        -> new BigDecimal("18.0");
            case FAIR        -> new BigDecimal("23.0");
            case POOR        -> new BigDecimal("28.0");
        };
    }

    // حداکثر مبلغ وام پیشنهادی (بر اساس ضریب درآمد ماهانه)
    public double getLoanMultiplier() {
        return switch (grade) {
            case EXCEPTIONAL -> 60.0;
            case VERY_GOOD   -> 48.0;
            case GOOD        -> 36.0;
            case FAIR        -> 24.0;
            case POOR        -> 12.0;
        };
    }

    public boolean needsRecalculation() {
        return nextRecalculationAt == null ||
               LocalDateTime.now().isAfter(nextRecalculationAt);
    }

    public static CreditGrade determineGrade(int score) {
        if (score >= 800) return CreditGrade.EXCEPTIONAL;
        if (score >= 740) return CreditGrade.VERY_GOOD;
        if (score >= 670) return CreditGrade.GOOD;
        if (score >= 580) return CreditGrade.FAIR;
        return CreditGrade.POOR;
    }
}
