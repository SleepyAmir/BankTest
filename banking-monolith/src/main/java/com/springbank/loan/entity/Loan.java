package com.springbank.loan.entity;
import com.springbank.account.entity.Account;
import com.springbank.user.entity.User;
import com.springbank.common.entity.BaseEntity;

import com.springbank.common.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Loan extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    // نرخ بهره سالانه (%) - بر اساس CreditScore تعیین می‌شود
    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    // مبلغ قسط ماهانه (محاسبه شده)
    @Column(name = "monthly_installment", precision = 19, scale = 4)
    private BigDecimal monthlyInstallment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LoanStatus status = LoanStatus.PENDING;
    // PENDING, APPROVED, REJECTED, ACTIVE, COMPLETED, DEFAULTED

    // هدف وام
    @Column(length = 200)
    private String purpose;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // username کارمندی که تأیید کرد
    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "rejection_reason", length = 300)
    private String rejectionReason;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // مانده بدهی
    @Column(name = "remaining_amount", precision = 19, scale = 4)
    private BigDecimal remainingAmount;

    // ======== RELATIONS ========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // حسابی که مبلغ وام به آن واریز می‌شود
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // امتیاز اعتباری لحظه درخواست (snapshot)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_score_id")
    private CreditScore creditScore;

    // جدول اقساط
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("installmentNumber ASC")
    private List<LoanInstallment> installments;

    // ======== HELPER METHODS ========

    // فرمول محاسبه قسط (PMT)
    public BigDecimal calculateMonthlyInstallment() {
        if (interestRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount.divide(new BigDecimal(durationMonths));
        }
        double r = interestRate.doubleValue() / 100 / 12;
        double n = durationMonths;
        double pmt = amount.doubleValue() * (r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
        return new BigDecimal(pmt).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    public boolean isActive() {
        return status == LoanStatus.ACTIVE;
    }

//    public long getPaidInstallmentsCount() {
//        if (installments == null) return 0;
//        return installments.stream()
//                .filter(i -> i.getStatus() == com.springbank.common.enums.InstallmentStatus.PAID)
//                .count();
//    }
}
