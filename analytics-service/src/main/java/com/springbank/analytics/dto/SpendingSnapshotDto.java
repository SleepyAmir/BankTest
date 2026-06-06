package com.springbank.analytics.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record SpendingSnapshotDto(
    Long id,
    YearMonth snapshotMonth,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    String categoryBreakdown,
    String topCategory,
    BigDecimal comparedToPrevMonth,
    BigDecimal savingsRate,
    Integer transactionCount,
    Long userId
) {}
