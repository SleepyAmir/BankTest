package com.springbank.fraud.dto;

import com.springbank.common.enums.FraudRiskLevel;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FraudAlertDto(
    Long id,
    BigDecimal riskScore,
    FraudRiskLevel riskLevel,
    List<String> triggeredRules,
    String deviceFingerprint,
    String ipAddress,
    String location,
    String reviewedBy,
    String reviewNote,
    LocalDateTime resolvedAt,
    Boolean userConfirmed,
    Long transactionId,
    Long userId,
    LocalDateTime createdAt
) {}
