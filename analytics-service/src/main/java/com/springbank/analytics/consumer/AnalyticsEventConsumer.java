package com.springbank.analytics.consumer;

import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * ANALYTICS EVENT CONSUMER
 * ============================================================================
 * Listens to: analytics.queue (routing: transaction.*)
 * Source: transaction-write publishes after creating/completing a transaction
 * Action: Updates SpendingSnapshot for the user (income vs expense aggregation)
 *
 * LOG MARKERS: [ANALYTICS-RECV] [ANALYTICS-PROC] [ANALYTICS-ERR]
 * ============================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventConsumer {

    private final AnalyticsService analyticsService;

    @RabbitListener(queues = "analytics.queue")
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("[ANALYTICS-RECV] ==================== ANALYTICS UPDATE ====================");
        log.info("[ANALYTICS-RECV] Received TransactionCompletedEvent: txId={}, userId={}, amount={}, type={}, status={}",
                event.getTransactionId(), event.getUserId(), event.getAmount(), event.getType(), event.getStatus());

        if (event.getUserId() == null) {
            log.warn("[ANALYTICS-RECV] ⚠️ userId is null — cannot update analytics snapshot. Skipping.");
            return;
        }

        try {
            log.info("[ANALYTICS-PROC] Calling AnalyticsService.processTransactionEvent(userId={}, amount={})...",
                    event.getUserId(), event.getAmount());
            analyticsService.processTransactionEvent(event);
            log.info("[ANALYTICS-PROC] ✅ SpendingSnapshot updated for userId={}", event.getUserId());
            log.info("[ANALYTICS-PROC] ✅ Query via GET /api/analytics/user/{} to see results", event.getUserId());
        } catch (Exception e) {
            log.error("[ANALYTICS-ERR] ❌ Failed to process analytics event for userId={}: {}", event.getUserId(), e.getMessage());
            log.error("[ANALYTICS-ERR] ⚠️ Analytics snapshot may be out of date. Check if analytics DB (port 5435) is running!");
            throw e;
        }
        log.info("[ANALYTICS-RECV] ==================== ANALYTICS DONE ====================\n");
    }
}
