package com.springbank.account.entity;

import com.springbank.common.entity.BaseEntity;
import com.springbank.common.enums.AccountStatus;
import com.springbank.common.enums.AccountType;
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
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(this.balance) > 0) {
            throw new IllegalStateException("موجودی کافی نیست");
        }
        this.balance = this.balance.subtract(amount);
    }
}
