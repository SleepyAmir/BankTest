package com.springbank.account.dto;

import com.springbank.common.enums.AccountStatus;
import java.math.BigDecimal;

public record AccountUpdateDto(
    String alias,
    AccountStatus status,
    BigDecimal dailyTransferLimit,
    BigDecimal monthlyTransferLimit
) {}
