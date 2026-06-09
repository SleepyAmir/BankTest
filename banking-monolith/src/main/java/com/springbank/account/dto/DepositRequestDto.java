package com.springbank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * درخواست شارژ حساب از طریق درگاه (فلوی ۴).
 */
public record DepositRequestDto(

        @NotNull(message = "شناسه‌ی حساب الزامی است")
        Long accountId,

        @NotNull(message = "مبلغ الزامی است")
        @DecimalMin(value = "0.01", message = "مبلغ باید بزرگ‌تر از صفر باشد")
        BigDecimal amount,

        /** دسته‌بندی هزینه/درآمد (مثلاً salary). */
        String spendingCategory,

        String description
) {}
