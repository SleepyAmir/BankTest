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

/**
 * ============================================================================
 * ANALYTICS SERVICE — Spending Snapshot Aggregation
 * ============================================================================
 * Triggered by: RabbitMQ event from transaction-write (via analytics.queue)
 * Logic: Aggregates income/expense per user per month (YearMonth)
 * Output: SpendingSnapshot entity with totals, count, savings rate, top category
 *
 * LOG MARKERS: [ANALYTICS-PROC] [ANALYTICS-INCOME] [ANALYTICS-EXPENSE] [ANALYTICS-SAVE]
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final SpendingSnapshotRepository spendingSnapshotRepository;
    private final SpendingSnapshotMapper spendingSnapshotMapper;

    @Transactional
    public void processTransactionEvent(TransactionCompletedEvent event) {
        log.info("[ANALYTICS-PROC] Processing transaction for analytics: userId={}, amount={}, type={}",
                event.getUserId(), event.getAmount(), event.getType());

        if (event.getTimestamp() == null) {
            log.warn("[ANALYTICS-PROC] ⚠️ Transaction timestamp is null — using current time");
        }
        YearMonth month = YearMonth.from(event.getTimestamp() != null ? event.getTimestamp() : java.time.LocalDateTime.now());
        log.info("[ANALYTICS-PROC] Snapshot month: {} for userId={}", month, event.getUserId());

        // Find existing snapshot or create new one
        SpendingSnapshot snapshot = spendingSnapshotRepository
                .findByUserIdAndSnapshotMonth(event.getUserId(), month)
                .orElseGet(() -> {
                    log.info("[ANALYTICS-PROC] No existing snapshot for userId={}, month={}. Creating new one.",
                            event.getUserId(), month);
                    return SpendingSnapshot.builder()
                            .userId(event.getUserId())
                            .snapshotMonth(month)
                            .totalIncome(BigDecimal.ZERO)
                            .totalExpense(BigDecimal.ZERO)
                            .transactionCount(0)
                            .build();
                });

        // Update totals based on transaction type
        if (isIncome(event)) {
            BigDecimal oldIncome = snapshot.getTotalIncome();
            snapshot.setTotalIncome(snapshot.getTotalIncome().add(event.getAmount()));
            log.info("[ANALYTICS-INCOME] +{} income. TotalIncome: {} → {} for userId={}",
                    event.getAmount(), oldIncome, snapshot.getTotalIncome(), event.getUserId());
        } else {
            BigDecimal oldExpense = snapshot.getTotalExpense();
            snapshot.setTotalExpense(snapshot.getTotalExpense().add(event.getAmount()));
            log.info("[ANALYTICS-EXPENSE] +{} expense. TotalExpense: {} → {} for userId={}",
                    event.getAmount(), oldExpense, snapshot.getTotalExpense(), event.getUserId());
        }
        snapshot.setTransactionCount(snapshot.getTransactionCount() + 1);

        // Update top category (last seen)
        if (event.getSpendingCategory() != null) {
            snapshot.setTopCategory(event.getSpendingCategory());
            log.info("[ANALYTICS-PROC] Top category updated to: {}", event.getSpendingCategory());
        }

        snapshot.calculateSavingsRate();
        calculateComparedToPrevMonth(snapshot);
        spendingSnapshotRepository.save(snapshot);
        log.info("[ANALYTICS-SAVE] ✅ Snapshot saved for userId={}, month={}. TotalIncome={}, TotalExpense={}, TxCount={}, SavingsRate={}",
                snapshot.getUserId(), snapshot.getSnapshotMonth(),
                snapshot.getTotalIncome(), snapshot.getTotalExpense(),
                snapshot.getTransactionCount(), snapshot.getSavingsRate());
    }

    private boolean isIncome(TransactionCompletedEvent event) {
        return event.getType() != null && (
                event.getType().equals("DEPOSIT") ||
                        event.getType().equals("SALARY") ||
                        event.getType().equals("REFUND") ||
                        event.getType().equals("LOAN_DISBURSEMENT")
        );
    }

    /**
     * محاسبه‌ی درصد تغییر هزینه نسبت به ماه قبل (compared_to_prev_month) — فلوی ۱۲.
     * مقدار مثبت = افزایش هزینه نسبت به ماه قبل.
     */
    private void calculateComparedToPrevMonth(SpendingSnapshot snapshot) {
        YearMonth prevMonth = snapshot.getSnapshotMonth().minusMonths(1);
        spendingSnapshotRepository.findByUserIdAndSnapshotMonth(snapshot.getUserId(), prevMonth)
                .ifPresent(prev -> {
                    BigDecimal prevExpense = prev.getTotalExpense();
                    if (prevExpense != null && prevExpense.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal change = snapshot.getTotalExpense().subtract(prevExpense)
                                .divide(prevExpense, 4, java.math.RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                        snapshot.setComparedToPrevMonth(change);
                    }
                });
    }

    @Transactional(readOnly = true)
    public List<SpendingSnapshotDto> getByUserId(Long userId) {
        log.info("[ANALYTICS-QUERY] Querying snapshots for userId={}", userId);
        List<SpendingSnapshotDto> result = spendingSnapshotRepository.findByUserIdOrderBySnapshotMonthDesc(userId).stream()
                .map(spendingSnapshotMapper::toDto)
                .collect(Collectors.toList());
        log.info("[ANALYTICS-QUERY] Found {} snapshots for userId={}", result.size(), userId);
        return result;
    }

    @Transactional(readOnly = true)
    public SpendingSnapshotDto getByUserAndMonth(Long userId, YearMonth month) {
        log.info("[ANALYTICS-QUERY] Querying snapshot for userId={}, month={}", userId, month);
        return spendingSnapshotRepository.findByUserIdAndSnapshotMonth(userId, month)
                .map(spendingSnapshotMapper::toDto)
                .orElseThrow(() -> {
                    log.error("[ANALYTICS-QUERY] ❌ Snapshot not found for userId={}, month={}", userId, month);
                    return new RuntimeException("Snapshot not found for userId=" + userId + " month=" + month);
                });
    }

    /**
     * نهایی‌سازی اسنپ‌شات‌های یک ماه: محاسبه‌ی مجدد savingsRate و comparedToPrevMonth.
     * توسط تسک زمان‌بندی‌شده‌ی پایان ماه فراخوانی می‌شود (فلوی ۱۲).
     *
     * @param month ماه هدف
     * @return تعداد اسنپ‌شات‌های نهایی‌شده
     */
    @Transactional
    public int finalizeMonth(YearMonth month) {
        log.info("[ANALYTICS-FINALIZE] نهایی‌سازی اسنپ‌شات‌های ماه {}", month);
        List<SpendingSnapshot> snapshots = spendingSnapshotRepository.findAll().stream()
                .filter(s -> month.equals(s.getSnapshotMonth()))
                .collect(Collectors.toList());

        for (SpendingSnapshot s : snapshots) {
            s.calculateSavingsRate();
            calculateComparedToPrevMonth(s);
            spendingSnapshotRepository.save(s);
        }
        log.info("[ANALYTICS-FINALIZE] ✅ {} اسنپ‌شات برای ماه {} نهایی شد", snapshots.size(), month);
        return snapshots.size();
    }

    @Transactional(readOnly = true)
    public List<SpendingSnapshotDto> getAllSnapshots() {
        log.info("[ANALYTICS-QUERY] Querying all snapshots");
        List<SpendingSnapshotDto> result = spendingSnapshotRepository.findAll().stream()
                .map(spendingSnapshotMapper::toDto)
                .collect(Collectors.toList());
        log.info("[ANALYTICS-QUERY] Found {} total snapshots", result.size());
        return result;
    }
}
