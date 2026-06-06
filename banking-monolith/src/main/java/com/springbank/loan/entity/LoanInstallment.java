package com.springbank.loan.entity;
import com.springbank.common.entity.BaseEntity;

import com.springbank.common.enums.InstallmentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_installments")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class LoanInstallment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    // مبلغ کل قسط
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    // سهم اصل وام در این قسط
    @Column(name = "principal_part", precision = 19, scale = 4)
    private BigDecimal principalPart;

    // سهم سود در این قسط
    @Column(name = "interest_part", precision = 19, scale = 4)
    private BigDecimal interestPart;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InstallmentStatus status = InstallmentStatus.PENDING;
    // PENDING, PAID, OVERDUE, WAIVED

    // Credit Scoring: جریمه دیرکرد
    @Column(name = "late_fee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal lateFee = BigDecimal.ZERO;

    // تعداد روزهای تأخیر
    @Column(name = "days_overdue")
    @Builder.Default
    private Integer daysOverdue = 0;

    // ======== RELATIONS ========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    // ======== HELPER METHODS ========

    public boolean isOverdue() {
        return status == InstallmentStatus.PENDING
                && LocalDate.now().isAfter(dueDate);
    }

    public boolean isPaidOnTime() {
        return status == InstallmentStatus.PAID
                && paidDate != null
                && !paidDate.isAfter(dueDate);
    }

    // محاسبه جریمه دیرکرد (2% ماهانه)
    public BigDecimal calculateLateFee() {
        if (!isOverdue()) return BigDecimal.ZERO;
        long days = java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        double feeRate = 0.02 / 30; // نرخ روزانه
        return amount.multiply(new BigDecimal(days * feeRate))
                .setScale(4, java.math.RoundingMode.HALF_UP);
    }
}
