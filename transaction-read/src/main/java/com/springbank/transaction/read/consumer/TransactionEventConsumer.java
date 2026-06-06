package com.springbank.transaction.read.consumer;

import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.transaction.read.entity.Transaction;
import com.springbank.transaction.read.mapper.TransactionMapper;
import com.springbank.transaction.read.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * TRANSACTION READ EVENT CONSUMER — CQRS Read Model Sync
 * ============================================================================
 * Listens to: transaction.read.queue (routing: transaction.completed)
 * Source: transaction-write publishes after creating/completing a transaction
 * Action: Saves to Read DB (port 5433) so queries are fast and separate from write load
 *
 * LOG MARKERS: [TX-READ-RECV] [TX-READ-DUP] [TX-READ-SAVE] [TX-READ-ERR]
 * ============================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @RabbitListener(queues = "transaction.read.queue")
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("[TX-READ-RECV] ==================== CQRS READ SYNC ====================");
        log.info("[TX-READ-RECV] Received TransactionCompletedEvent: trackingCode={}, txId={}, amount={}, status={}",
                event.getTrackingCode(), event.getTransactionId(), event.getAmount(), event.getStatus());

        // Deduplication: avoid saving the same transaction twice
        if (transactionRepository.findByTrackingCode(event.getTrackingCode()).isPresent()) {
            log.info("[TX-READ-DUP] ⚠️ Transaction already exists in read model. Skipping: trackingCode={}",
                    event.getTrackingCode());
            return;
        }

        log.info("[TX-READ-SAVE] New transaction — building entity from event...");
        try {
            Transaction tx = Transaction.builder()
                    .trackingCode(event.getTrackingCode())
                    .amount(event.getAmount())
                    .type(event.getType() != null ? com.springbank.common.enums.TransactionType.valueOf(event.getType()) : null)
                    .status(event.getStatus() != null ? com.springbank.common.enums.TransactionStatus.valueOf(event.getStatus()) : com.springbank.common.enums.TransactionStatus.PENDING)
                    .currency("IRR")
                    .description("Synced from event")
                    .spendingCategory(event.getSpendingCategory())
                    .ipAddress(event.getIpAddress())
                    .deviceFingerprint(event.getDeviceFingerprint())
                    .location(event.getLocation())
                    .fromAccountId(event.getFromAccountId())
                    .toAccountId(event.getToAccountId())
                    .cardId(event.getCardId())
                    .build();

            transactionRepository.save(tx);
            log.info("[TX-READ-SAVE] ✅ Transaction saved to READ DB: trackingCode={}", event.getTrackingCode());
            log.info("[TX-READ-SAVE] ✅ Now available at GET /api/transactions/tracking/{}", event.getTrackingCode());
        } catch (Exception e) {
            log.error("[TX-READ-ERR] ❌ Failed to save transaction to read model: {}", e.getMessage());
            log.error("[TX-READ-ERR] ⚠️ Event lost! Read DB is out of sync with write DB.");
            throw e; // Re-throw so RabbitMQ can retry (if configured)
        }
        log.info("[TX-READ-RECV] ==================== SYNC COMPLETE ====================\n");
    }
}
