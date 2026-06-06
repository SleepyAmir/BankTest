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
        // In production: iterate all users and refresh their monthly snapshots
    }
}
