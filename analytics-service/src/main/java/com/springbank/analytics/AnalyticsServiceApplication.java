package com.springbank.analytics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Analytics Service
 * Port: 8093 | DB: 5435 | RabbitMQ Consumer: analytics.queue (transaction.*)
 * Flow: Receives TX Event → Updates SpendingSnapshot → Daily Scheduled Refresh
 */
@Slf4j
@SpringBootApplication
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        log.info("🚀 [STEP 6/7] Starting ANALYTICS-SERVICE on port 8093...");
        log.info("🔗 Depends on: RabbitMQ (5672), PostgreSQL (5435)");
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup() {
        log.info("✅ [STEP 6/7] ✅ ANALYTICS-SERVICE is UP on port 8093");
        log.info("📋 Swagger UI: http://localhost:8093/swagger-ui.html");
        log.info("⚠️  If analytics snapshots are empty → Send a transaction event via RabbitMQ first!");
    }
}
