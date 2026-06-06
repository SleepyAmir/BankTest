package com.springbank.common.event;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FraudDetectedEvent {
    private Long fraudAlertId;
    private Long transactionId;
    private Long userId;
    private BigDecimal riskScore;
    private String riskLevel;
    private List<String> triggeredRules;
    private LocalDateTime detectedAt;
}
