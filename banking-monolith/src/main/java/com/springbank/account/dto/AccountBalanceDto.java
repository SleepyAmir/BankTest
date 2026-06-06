package com.springbank.account.dto;

import java.math.BigDecimal;

public record AccountBalanceDto(
    Long accountId,
    String accountNumber,
    BigDecimal balance
) {}
