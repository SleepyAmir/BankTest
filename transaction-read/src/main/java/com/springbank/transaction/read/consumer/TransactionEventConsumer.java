package com.springbank.transaction.read.consumer;

import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.transaction.read.entity.Transaction;
import com.springbank.transaction.read.mapper.TransactionMapper;
import com.springbank.transaction.read.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @RabbitListener(queues = "transaction.read.queue")
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("Transaction-read received event: {}", event.getTrackingCode());

        // Avoid duplicates
        if (transactionRepository.findByTrackingCode(event.getTrackingCode()).isPresent()) {
            log.debug("Transaction already exists in read model, skipping: {}", event.getTrackingCode());
            return;
        }

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
        log.info("Transaction saved to read model: {}", event.getTrackingCode());
    }
}
