package com.springbank.account.dto;

/**
 * اطلاعات حداقلی حساب برای «یافتن مقصد انتقال» با شماره حساب.
 * عمداً موجودی و جزئیات حساس را برنمی‌گرداند.
 */
public record AccountLookupDto(
        Long id,
        String accountNumber,
        String ownerName,
        String status
) {}
