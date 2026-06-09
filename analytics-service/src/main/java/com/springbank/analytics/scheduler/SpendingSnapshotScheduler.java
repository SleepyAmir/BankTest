package com.springbank.analytics.scheduler;

import com.springbank.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * ============================================================================
 * SPENDING SNAPSHOT SCHEDULER — تسک زمان‌بندی‌شده‌ی مالی شخصی (فلوی ۱۲)
 * ============================================================================
 * در پایان هر ماه (آخرین روز، ساعت ۲۳:۵۹) اسنپ‌شات‌های آن ماه نهایی می‌شوند:
 * savingsRate و comparedToPrevMonth بازمحاسبه می‌گردند.
 *
 * نکته: اسنپ‌شات‌ها به‌صورت لحظه‌ای با هر تراکنش (از طریق رویداد) ساخته/به‌روزرسانی
 * می‌شوند؛ این تسک فقط مقادیر تجمیعی پایان دوره را قطعی می‌کند.
 * ============================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpendingSnapshotScheduler {

    private final AnalyticsService analyticsService;

    /** آخرین روز هر ماه، ساعت ۲۳:۵۹ → نهایی‌سازی اسنپ‌شات‌های ماه جاری. */
    @Scheduled(cron = "0 59 23 L * ?")
    public void finalizeCurrentMonth() {
        YearMonth current = YearMonth.now();
        log.info("[ANALYTICS-CRON] شروع نهایی‌سازی پایان ماه: {}", current);
        int count = analyticsService.finalizeMonth(current);
        log.info("[ANALYTICS-CRON] ✅ نهایی‌سازی کامل شد. {} اسنپ‌شات برای ماه {}", count, current);
    }
}
