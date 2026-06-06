package com.springbank.fraud.dto;

import com.springbank.common.enums.AlertSeverity;
import com.springbank.common.enums.AlertStatus;
import com.springbank.common.enums.AmlAlertType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AmlAlertDto(
    Long id,
    AmlAlertType type,
    AlertSeverity severity,
    AlertStatus status,
    BigDecimal riskScore,
    String description,
    String investigatorNote,
    String investigatorUsername,
    LocalDateTime resolvedAt,
    Long userId,
    Long transactionId,
    LocalDateTime createdAt
) {}
