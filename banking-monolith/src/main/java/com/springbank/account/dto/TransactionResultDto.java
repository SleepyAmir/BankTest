package com.springbank.account.dto;

import java.math.BigDecimal;

/**
 * نتیجه‌ی یک عملیات پولی (شارژ یا انتقال) — شامل کد پیگیری و موجودی نهایی.
 */
public record TransactionResultDto(
        String trackingCode,
        String type,
        String status,
        BigDecimal amount,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal fromBalanceAfter,
        BigDecimal toBalanceAfter
) {}
