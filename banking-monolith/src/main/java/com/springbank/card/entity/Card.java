package com.springbank.card.entity;

import com.springbank.account.entity.Account;
import com.springbank.common.entity.BaseEntity;
import com.springbank.common.enums.CardStatus;
import com.springbank.common.enums.CardType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Card extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", nullable = false, unique = true, length = 16)
    private String cardNumber;

    @Column(name = "cvv2", nullable = false, length = 100)
    private String cvv2;

    @Column(name = "pin", length = 100)
    private String pin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType type; // DEBIT, CREDIT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CardStatus status = CardStatus.ACTIVE; // ACTIVE, BLOCKED, EXPIRED

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "is_contactless")
    @Builder.Default
    private Boolean isContactless = true;

    @Column(name = "daily_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyLimit = new BigDecimal("10000000");

    @Column(name = "monthly_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlyLimit = new BigDecimal("50000000");

    @Column(name = "monthly_spent", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlySpent = BigDecimal.ZERO;

    // ======== RELATIONS (Monolith-internal only) ========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // ======== HELPER METHODS ========

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now());
    }

    public boolean isUsable() {
        return status == CardStatus.ACTIVE && !isExpired();
    }

    public boolean hasEnoughDailyLimit(BigDecimal amount) {
        return dailyLimit.compareTo(amount) >= 0;
    }
}
