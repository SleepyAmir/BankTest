package com.springbank.transaction.write.dto.request;

import com.springbank.common.enums.TransactionType;
import java.math.BigDecimal;

public record TransactionCreateDto(
    BigDecimal amount,
    String currency,
    TransactionType type,
    String description,
    Long fromAccountId,
    Long toAccountId,
    Long cardId,
    Long loanInstallmentId,
    String spendingCategory,
    String ipAddress,
    String deviceFingerprint,
    String location
) {}
