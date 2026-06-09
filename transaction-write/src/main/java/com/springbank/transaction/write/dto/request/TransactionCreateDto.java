package com.springbank.transaction.write.dto.request;

import com.springbank.common.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransactionCreateDto(

        @NotNull(message = "مبلغ الزامی است")
        @DecimalMin(value = "0.01", message = "مبلغ باید بزرگ‌تر از صفر باشد")
        BigDecimal amount,

        String currency,

        @NotNull(message = "نوع تراکنش الزامی است")
        TransactionType type,

        String description,
        Long fromAccountId,
        Long toAccountId,
        Long cardId,
        Long loanInstallmentId,
        String spendingCategory,
        String ipAddress,
        String deviceFingerprint,
        String location,
        Long userId
) {}
