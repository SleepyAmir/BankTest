package com.springbank.account.entity;

import com.springbank.card.entity.Card;
import com.springbank.common.entity.BaseEntity;
import com.springbank.common.enums.AccountStatus;
import com.springbank.common.enums.AccountType;
import com.springbank.loan.entity.Loan;
import com.springbank.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 26)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type; // CHECKING, SAVINGS

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE; // ACTIVE, FROZEN, CLOSED

    @Column(length = 50)
    private String alias;

    @Column(name = "daily_transfer_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyTransferLimit = new BigDecimal("5000000");

    @Column(name = "monthly_transfer_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlyTransferLimit = new BigDecimal("20000000");

    // ======== مصرف انتقال (برای کنترل سقف روزانه/ماهانه) ========

    @Column(name = "daily_transferred", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyTransferred = BigDecimal.ZERO;

    @Column(name = "monthly_transferred", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlyTransferred = BigDecimal.ZERO;

    @Column(name = "last_transfer_at")
    private java.time.LocalDateTime lastTransferAt;

    // ======== RELATIONS (Monolith-internal only) ========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<Card> cards;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<Loan> loans;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<OpenBankingConsent> consents;

    // ======== HELPER METHODS ========

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);
        if (amount.compareTo(this.balance) > 0) {
            throw new IllegalStateException("موجودی کافی نیست");
        }
        this.balance = this.balance.subtract(amount);
    }

    /** اعتبارسنجی دامنه‌ای: مبلغ باید مثبت و غیرصفر باشد (دفاع لایه‌ای در کنار validation کنترلر). */
    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("مبلغ باید بزرگ‌تر از صفر باشد");
        }
    }

    /**
     * بازنشانی شمارنده‌های مصرف بر اساس زمان آخرین انتقال:
     *  - اگر روز تغییر کرده باشد، مصرف روزانه صفر می‌شود.
     *  - اگر ماه تغییر کرده باشد، مصرف ماهانه صفر می‌شود.
     */
    public void resetTransferCountersIfNeeded() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (lastTransferAt == null) {
            return;
        }
        if (lastTransferAt.toLocalDate().isBefore(now.toLocalDate())) {
            this.dailyTransferred = BigDecimal.ZERO;
        }
        if (lastTransferAt.getYear() != now.getYear() || lastTransferAt.getMonthValue() != now.getMonthValue()) {
            this.monthlyTransferred = BigDecimal.ZERO;
        }
    }

    /** آیا با احتساب این مبلغ، سقف روزانه‌ی مؤثر رعایت می‌شود؟ */
    public boolean isWithinDailyLimit(BigDecimal amount, BigDecimal effectiveDailyLimit) {
        return dailyTransferred.add(amount).compareTo(effectiveDailyLimit) <= 0;
    }

    /** آیا با احتساب این مبلغ، سقف ماهانه‌ی مؤثر رعایت می‌شود؟ */
    public boolean isWithinMonthlyLimit(BigDecimal amount, BigDecimal effectiveMonthlyLimit) {
        return monthlyTransferred.add(amount).compareTo(effectiveMonthlyLimit) <= 0;
    }

    /** ثبت یک انتقال خروجی در شمارنده‌های مصرف. */
    public void registerOutgoingTransfer(BigDecimal amount) {
        this.dailyTransferred = this.dailyTransferred.add(amount);
        this.monthlyTransferred = this.monthlyTransferred.add(amount);
        this.lastTransferAt = java.time.LocalDateTime.now();
    }
}
