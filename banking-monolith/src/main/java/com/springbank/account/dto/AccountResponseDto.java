package com.springbank.account.dto;

import com.springbank.common.enums.AccountStatus;
import com.springbank.common.enums.AccountType;
import java.math.BigDecimal;

public record AccountResponseDto(
    Long id,
    String accountNumber,
    AccountType type,
    BigDecimal balance,
    AccountStatus status,
    String alias,
    BigDecimal dailyTransferLimit,
    BigDecimal monthlyTransferLimit,
    Long userId,
    Long branchId
) {}
