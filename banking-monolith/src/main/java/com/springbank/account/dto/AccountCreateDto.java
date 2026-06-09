package com.springbank.account.dto;

import com.springbank.common.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountCreateDto(

        @NotBlank(message = "شماره حساب الزامی است")
        String accountNumber,

        @NotNull(message = "نوع حساب الزامی است")
        AccountType type,

        String alias,
        BigDecimal dailyTransferLimit,
        BigDecimal monthlyTransferLimit,

        @NotNull(message = "شناسه‌ی کاربر الزامی است")
        Long userId,

        Long branchId
) {}
