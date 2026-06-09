package com.springbank.card.dto;

import com.springbank.common.enums.CardStatus;
import com.springbank.common.enums.CardType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * نتیجه‌ی صدور کارت جدید.
 * <p>
 * توجه امنیتی: {@code cvv2} و {@code pin} فقط «یک‌بار» هنگام صدور به‌صورت متن آشکار
 * برگردانده می‌شوند (مانند بانک‌های واقعی) و پس از آن در DB به‌صورت هش ذخیره شده و
 * هرگز دوباره قابل بازیابی نیستند.
 */
public record IssuedCardDto(
        Long id,
        String cardNumber,
        String cvv2,        // متن آشکار — فقط همین یک‌بار
        String pin,         // متن آشکار — فقط همین یک‌بار
        CardType type,
        CardStatus status,
        LocalDate expiryDate,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        Long accountId
) {}
