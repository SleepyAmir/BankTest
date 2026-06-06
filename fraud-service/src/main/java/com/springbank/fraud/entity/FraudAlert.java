package com.springbank.fraud.entity;

import com.springbank.common.entity.BaseEntity;
import com.springbank.common.enums.FraudRiskLevel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class FraudAlert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private FraudRiskLevel riskLevel;

    @Column(name = "triggered_rules", columnDefinition = "TEXT")
    private String triggeredRules;

    @Column(name = "device_fingerprint", length = 200)
    private String deviceFingerprint;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 100)
    private String location;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "user_confirmed")
    private Boolean userConfirmed;

    // ======== CROSS-SERVICE IDs (no JPA relations to other services) ========

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // ======== HELPER METHODS ========

    public boolean shouldBlock() {
        return riskLevel == FraudRiskLevel.BLOCK;
    }

    public boolean requiresOtp() {
        return riskLevel == FraudRiskLevel.CHALLENGE;
    }

    public boolean isHighRisk() {
        return riskScore.compareTo(new BigDecimal("60")) >= 0;
    }
}
