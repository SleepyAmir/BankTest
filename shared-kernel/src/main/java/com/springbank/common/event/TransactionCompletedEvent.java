package com.springbank.common.event;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionCompletedEvent {
    private Long transactionId;
    private String trackingCode;
    private Long fromAccountId;
    private Long toAccountId;
    private Long cardId;
    private Long userId;
    private BigDecimal amount;
    private String type;
    private String status;
    private String spendingCategory;
    private String ipAddress;
    private String deviceFingerprint;
    private String location;
    private LocalDateTime timestamp;
}
