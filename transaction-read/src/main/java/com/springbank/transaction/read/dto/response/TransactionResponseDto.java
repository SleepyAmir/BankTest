package com.springbank.transaction.read.dto.response;

import com.springbank.common.enums.TransactionStatus;
import com.springbank.common.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDto(
    Long id,
    String trackingCode,
    BigDecimal amount,
    String currency,
    TransactionType type,
    TransactionStatus status,
    String description,
    String referenceNo,
    String spendingCategory,
    Long fromAccountId,
    Long toAccountId,
    Long cardId,
    Long loanInstallmentId,
    String ipAddress,
    String deviceFingerprint,
    String location,
    LocalDateTime createdAt
) {}
