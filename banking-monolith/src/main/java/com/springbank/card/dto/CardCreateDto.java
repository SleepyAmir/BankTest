package com.springbank.card.dto;

import com.springbank.common.enums.CardType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CardCreateDto(
    String cardNumber,
    String cvv2,
    String pin,
    CardType type,
    LocalDate expiryDate,
    Boolean isContactless,
    BigDecimal dailyLimit,
    BigDecimal monthlyLimit,
    Long accountId
) {}
