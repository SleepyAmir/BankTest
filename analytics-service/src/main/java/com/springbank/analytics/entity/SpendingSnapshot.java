package com.springbank.analytics.entity;

import com.springbank.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "spending_snapshots",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "snapshot_month"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SpendingSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_month", nullable = false)
    private YearMonth snapshotMonth;

    @Column(name = "total_income", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Column(name = "total_expense", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @Column(name = "category_breakdown", columnDefinition = "TEXT")
    private String categoryBreakdown;

    @Column(name = "top_category", length = 50)
    private String topCategory;

    @Column(name = "compared_to_prev_month", precision = 6, scale = 2)
    private BigDecimal comparedToPrevMonth;

    @Column(name = "savings_rate", precision = 6, scale = 2)
    private BigDecimal savingsRate;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    // ======== CROSS-SERVICE ID (no JPA relation to monolith) ========

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // ======== HELPER METHODS ========

    public BigDecimal getNetCashFlow() {
        return totalIncome.subtract(totalExpense);
    }

    public boolean isPositiveCashFlow() {
        return getNetCashFlow().compareTo(BigDecimal.ZERO) > 0;
    }

    public void calculateSavingsRate() {
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            this.savingsRate = BigDecimal.ZERO;
            return;
        }
        this.savingsRate = getNetCashFlow()
                .divide(totalIncome, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
