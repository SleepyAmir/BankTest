package com.springbank.loan.dto;

import com.springbank.common.enums.InstallmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanInstallmentDto(
    Long id,
    Integer installmentNumber,
    BigDecimal amount,
    BigDecimal principalPart,
    BigDecimal interestPart,
    LocalDate dueDate,
    LocalDate paidDate,
    InstallmentStatus status,
    BigDecimal lateFee,
    Integer daysOverdue
) {}
