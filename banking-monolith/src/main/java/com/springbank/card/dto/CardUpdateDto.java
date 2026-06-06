package com.springbank.card.dto;

import com.springbank.common.enums.CardStatus;
import java.math.BigDecimal;

public record CardUpdateDto(
    CardStatus status,
    BigDecimal dailyLimit,
    BigDecimal monthlyLimit
) {}
