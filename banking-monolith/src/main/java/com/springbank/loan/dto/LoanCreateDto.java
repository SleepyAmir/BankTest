package com.springbank.loan.dto;

import java.math.BigDecimal;

public record LoanCreateDto(
    BigDecimal amount,
    Integer durationMonths,
    String purpose,
    Long userId,
    Long accountId
) {}
