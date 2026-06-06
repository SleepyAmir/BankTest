package com.springbank.fraud.consumer;

import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.fraud.service.FraudAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * FRAUD EVENT CONSUMER
 * ============================================================================
 * Listens to: fraud.queue (routing: transaction.*)
 * Source: transaction-write publishes after creating/completing a transaction
 * Action: Triggers fraud scoring rules and creates FraudAlert/AMLAlert
 *
 * LOG MARKERS: [FRAUD-RECV] [FRAUD-PROC] [FRAUD-ERR]
 * ============================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudEventConsumer {

    private final FraudAnalysisService fraudAnalysisService;

    @RabbitListener(queues = "fraud.queue")
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("[FRAUD-RECV] ==================== FRAUD CHECK ====================");
        log.info("[FRAUD-RECV] Received TransactionCompletedEvent: txId={}, trackingCode={}, amount={}, userId={}",
                event.getTransactionId(), event.getTrackingCode(), event.getAmount(), event.getUserId());

        try {
            log.info("[FRAUD-PROC] Running fraud analysis rules...");
            fraudAnalysisService.analyzeTransaction(event);
            log.info("[FRAUD-PROC] ✅ Fraud analysis complete for txId={}", event.getTransactionId());
            log.info("[FRAUD-PROC] ✅ Query via GET /api/fraud/alerts/user/{} or /api/fraud/alerts", event.getUserId());
        } catch (Exception e) {
            log.error("[FRAUD-ERR] ❌ Fraud analysis failed for txId={}: {}", event.getTransactionId(), e.getMessage());
            log.error("[FRAUD-ERR] ⚠️ Transaction was NOT evaluated for fraud! Check fraud DB (port 5434).");
            throw e;
        }
        log.info("[FRAUD-RECV] ==================== FRAUD DONE ====================\n");
    }
}
