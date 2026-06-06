package com.springbank.account.dto;

import com.springbank.common.enums.AccountType;
import java.math.BigDecimal;

public record AccountCreateDto(
    String accountNumber,
    AccountType type,
    String alias,
    BigDecimal dailyTransferLimit,
    BigDecimal monthlyTransferLimit,
    Long userId,
    Long branchId
) {}
