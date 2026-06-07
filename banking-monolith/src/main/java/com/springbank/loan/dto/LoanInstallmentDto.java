package com.springbank.loan.dto;

import com.springbank.common.enums.InstallmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanInstallmentDto(
        Long id,
        Long loanId,
        Integer installmentNumber,
        BigDecimal amount,
        BigDecimal principalPart,
        BigDecimal interestPart,   // ← اضافه شد
        LocalDate dueDate,
        LocalDate paidDate,
        InstallmentStatus status,
        BigDecimal lateFee,
        Integer daysOverdue
) {}