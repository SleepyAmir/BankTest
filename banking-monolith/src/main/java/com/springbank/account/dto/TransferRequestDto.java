package com.springbank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * درخواست انتقال وجه داخلی بین دو حساب در همان بانک (فلوی ۵).
 */
public record TransferRequestDto(

        @NotNull(message = "حساب مبدأ الزامی است")
        Long fromAccountId,

        @NotNull(message = "حساب مقصد الزامی است")
        Long toAccountId,

        @NotNull(message = "مبلغ الزامی است")
        @DecimalMin(value = "0.01", message = "مبلغ باید بزرگ‌تر از صفر باشد")
        BigDecimal amount,

        String spendingCategory,

        String description
) {}
