package com.springbank.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoanCreateDto(

        @NotNull(message = "مبلغ وام الزامی است")
        @DecimalMin(value = "1000000", message = "حداقل مبلغ وام ۱٬۰۰۰٬۰۰۰ است")
        BigDecimal amount,

        @NotNull(message = "مدت وام الزامی است")
        @Min(value = 1, message = "حداقل مدت وام ۱ ماه است")
        @Max(value = 120, message = "حداکثر مدت وام ۱۲۰ ماه است")
        Integer durationMonths,

        String purpose,

        @NotNull(message = "شناسه‌ی کاربر الزامی است")
        Long userId,

        @NotNull(message = "شناسه‌ی حساب الزامی است")
        Long accountId
) {}
