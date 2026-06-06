package com.springbank.transaction.write.entity;

import com.springbank.common.entity.BaseEntity;
import com.springbank.common.enums.TransactionStatus;
import com.springbank.common.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_code", nullable = false, unique = true, length = 30)
    private String trackingCode;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 5)
    @Builder.Default
    private String currency = "IRR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // DEPOSIT, WITHDRAWAL, TRANSFER, CARD_PAYMENT, REFUND

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(length = 300)
    private String description;

    @Column(name = "reference_no", length = 50)
    private String referenceNo;

    @Column(name = "spending_category", length = 50)
    private String spendingCategory;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_fingerprint", length = 200)
    private String deviceFingerprint;

    @Column(length = 100)
    private String location;

    // ======== CROSS-SERVICE IDs (no JPA relations to other services) ========

    @Column(name = "from_account_id")
    private Long fromAccountId;

    @Column(name = "to_account_id")
    private Long toAccountId;

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "loan_installment_id")
    private Long loanInstallmentId;

    @Column(name = "user_id")
    private Long userId;

    // ======== HELPER METHODS ========

    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    public boolean isBlocked() {
        return status == TransactionStatus.BLOCKED;
    }
}
