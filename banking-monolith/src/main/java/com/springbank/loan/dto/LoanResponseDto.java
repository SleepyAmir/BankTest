package com.springbank.loan.dto;

import com.springbank.common.enums.LoanStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoanResponseDto(
    Long id,
    BigDecimal amount,
    BigDecimal interestRate,
    Integer durationMonths,
    BigDecimal monthlyInstallment,
    LoanStatus status,
    String purpose,
    LocalDateTime approvedAt,
    String approvedBy,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal remainingAmount,
    Long userId,
    Long accountId
) {}
