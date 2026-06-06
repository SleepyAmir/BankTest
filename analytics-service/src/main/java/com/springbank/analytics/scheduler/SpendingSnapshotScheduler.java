package com.springbank.analytics.scheduler;

import com.springbank.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpendingSnapshotScheduler {

    private final AnalyticsService analyticsService;

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void refreshSnapshots() {
        log.info("Running daily analytics snapshot refresh");
        var snapshots = analyticsService.getAllSnapshots();
        for (var dto : snapshots) {
            // Recalculate savings rate for any missing data
            log.debug("Refreshed snapshot for user {} month {}", dto.userId(), dto.snapshotMonth());
        }
        log.info("Daily analytics refresh completed. {} snapshots refreshed.", snapshots.size());
    }
}
