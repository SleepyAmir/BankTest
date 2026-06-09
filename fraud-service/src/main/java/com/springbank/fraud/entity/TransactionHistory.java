package com.springbank.fraud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * TRANSACTION HISTORY — تاریخچه‌ی سبک تراکنش‌ها در fraud-service
 * ============================================================================
 * fraud-service نسخه‌ی محلی و سبکی از تراکنش‌های دریافتی (از TransactionCompletedEvent)
 * نگه می‌دارد تا قوانین وابسته به تاریخچه را مستقل و بدون فراخوانی همزمانِ سرویس‌های
 * دیگر محاسبه کند:
 *   - Velocity: تعداد تراکنش در یک بازه‌ی کوتاه.
 *   - Amount-Anomaly: مقایسه با میانگین ماهانه.
 *   - Structuring: چند تراکنش کوچک پشت سر هم.
 * ============================================================================
 */
@Entity
@Table(name = "fraud_transaction_history", indexes = {
        @Index(name = "idx_fth_user", columnList = "user_id"),
        @Index(name = "idx_fth_user_time", columnList = "user_id,occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "tracking_code", length = 40)
    private String trackingCode;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "from_account_id")
    private Long fromAccountId;

    @Column(name = "to_account_id")
    private Long toAccountId;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(length = 30)
    private String type;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
