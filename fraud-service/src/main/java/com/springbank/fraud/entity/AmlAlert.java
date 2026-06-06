package com.springbank.fraud.entity;

import com.springbank.common.entity.BaseEntity;
import com.springbank.common.enums.AlertSeverity;
import com.springbank.common.enums.AlertStatus;
import com.springbank.common.enums.AmlAlertType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "aml_alerts")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class AmlAlert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AmlAlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "investigator_note", length = 1000)
    private String investigatorNote;

    @Column(name = "investigator_username", length = 50)
    private String investigatorUsername;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ======== CROSS-SERVICE IDs (no JPA relations to other services) ========

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "transaction_id")
    private Long transactionId;

    // ======== HELPER METHODS ========

    public boolean isCritical() {
        return severity == AlertSeverity.CRITICAL;
    }

    public boolean requiresImmediateAction() {
        return (severity == AlertSeverity.HIGH || severity == AlertSeverity.CRITICAL)
                && status == AlertStatus.OPEN;
    }
}
