package com.springbank.card.dto;

import com.springbank.common.enums.CardStatus;
import com.springbank.common.enums.CardType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CardResponseDto(
        Long id,
        String cardNumber,
        String cvv2,          // متن آشکار (decrypt شده) — برای نمایش در کارت
        CardType type,
        CardStatus status,
        LocalDate expiryDate,
        Boolean isContactless,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        BigDecimal monthlySpent,
        Long accountId,
        Long userId
) {}
