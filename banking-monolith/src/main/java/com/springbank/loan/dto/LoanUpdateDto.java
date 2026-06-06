package com.springbank.loan.dto;

import java.math.BigDecimal;

public record LoanUpdateDto(
    BigDecimal interestRate,
    String purpose
) {}
