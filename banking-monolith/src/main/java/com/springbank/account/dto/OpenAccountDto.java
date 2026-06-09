package com.springbank.account.dto;

import com.springbank.common.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * درخواست افتتاح حساب توسط کاربر (فلوی ۳).
 * شماره حساب و موجودی اولیه توسط سیستم تعیین می‌شوند؛ کاربر فقط نوع و شعبه را انتخاب می‌کند.
 */
public record OpenAccountDto(

        @NotNull(message = "شناسه‌ی کاربر الزامی است")
        Long userId,

        @NotNull(message = "نوع حساب الزامی است (CHECKING یا SAVINGS)")
        AccountType type,

        @NotBlank(message = "کد شعبه الزامی است")
        String branchCode,

        String alias
) {}
