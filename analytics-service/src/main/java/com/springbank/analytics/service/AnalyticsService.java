package com.springbank.analytics.service;

import com.springbank.analytics.dto.SpendingSnapshotDto;
import com.springbank.analytics.entity.SpendingSnapshot;
import com.springbank.analytics.mapper.SpendingSnapshotMapper;
import com.springbank.analytics.repository.SpendingSnapshotRepository;
import com.springbank.common.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final SpendingSnapshotRepository spendingSnapshotRepository;
    private final SpendingSnapshotMapper spendingSnapshotMapper;

    @Transactional
    public void processTransactionEvent(TransactionCompletedEvent event) {
        YearMonth month = YearMonth.from(event.getTimestamp());

        SpendingSnapshot snapshot = spendingSnapshotRepository
                .findByUserIdAndSnapshotMonth(event.getUserId(), month)
                .orElseGet(() -> SpendingSnapshot.builder()
                        .userId(event.getUserId())
                        .snapshotMonth(month)
                        .totalIncome(BigDecimal.ZERO)
                        .totalExpense(BigDecimal.ZERO)
                        .transactionCount(0)
                        .build());

        // Update totals based on transaction type
        if (isIncome(event)) {
            snapshot.setTotalIncome(snapshot.getTotalIncome().add(event.getAmount()));
        } else {
            snapshot.setTotalExpense(snapshot.getTotalExpense().add(event.getAmount()));
        }
        snapshot.setTransactionCount(snapshot.getTransactionCount() + 1);

        // Update category
        if (event.getSpendingCategory() != null) {
            snapshot.setTopCategory(event.getSpendingCategory());
        }

        snapshot.calculateSavingsRate();
        spendingSnapshotRepository.save(snapshot);
        log.debug("Updated spending snapshot for user {} month {}", event.getUserId(), month);
    }

    private boolean isIncome(TransactionCompletedEvent event) {
        return event.getType().equals("DEPOSIT") || event.getType().equals("SALARY") || event.getType().equals("REFUND");
    }

    @Transactional(readOnly = true)
    public List<SpendingSnapshotDto> getByUserId(Long userId) {
        return spendingSnapshotRepository.findByUserIdOrderBySnapshotMonthDesc(userId).stream()
                .map(spendingSnapshotMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SpendingSnapshotDto getByUserAndMonth(Long userId, YearMonth month) {
        return spendingSnapshotRepository.findByUserIdAndSnapshotMonth(userId, month)
                .map(spendingSnapshotMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Snapshot not found"));
    }
}
