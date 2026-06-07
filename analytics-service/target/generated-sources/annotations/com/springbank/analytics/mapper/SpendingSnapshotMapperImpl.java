package com.springbank.analytics.mapper;

import com.springbank.analytics.dto.SpendingSnapshotDto;
import com.springbank.analytics.entity.SpendingSnapshot;
import java.math.BigDecimal;
import java.time.YearMonth;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-07T19:59:28+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class SpendingSnapshotMapperImpl implements SpendingSnapshotMapper {

    @Override
    public SpendingSnapshotDto toDto(SpendingSnapshot entity) {
        if ( entity == null ) {
            return null;
        }

        Long id = null;
        YearMonth snapshotMonth = null;
        BigDecimal totalIncome = null;
        BigDecimal totalExpense = null;
        String categoryBreakdown = null;
        String topCategory = null;
        BigDecimal comparedToPrevMonth = null;
        BigDecimal savingsRate = null;
        Integer transactionCount = null;
        Long userId = null;

        id = entity.getId();
        snapshotMonth = entity.getSnapshotMonth();
        totalIncome = entity.getTotalIncome();
        totalExpense = entity.getTotalExpense();
        categoryBreakdown = entity.getCategoryBreakdown();
        topCategory = entity.getTopCategory();
        comparedToPrevMonth = entity.getComparedToPrevMonth();
        savingsRate = entity.getSavingsRate();
        transactionCount = entity.getTransactionCount();
        userId = entity.getUserId();

        SpendingSnapshotDto spendingSnapshotDto = new SpendingSnapshotDto( id, snapshotMonth, totalIncome, totalExpense, categoryBreakdown, topCategory, comparedToPrevMonth, savingsRate, transactionCount, userId );

        return spendingSnapshotDto;
    }
}
