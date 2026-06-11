package com.springbank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * درخواست انتقال وجه «اتمیک» داخلی (سرویس‌به‌سرویس).
 * توسط transaction-write فراخوانی می‌شود و کل جابجایی پول در یک تراکنش DB انجام می‌گیرد.
 */
public record InternalTransferDto(

        @NotNull Long fromAccountId,
        @NotNull Long toAccountId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        BigDecimal fee,
        boolean enforceLimits
) {}
