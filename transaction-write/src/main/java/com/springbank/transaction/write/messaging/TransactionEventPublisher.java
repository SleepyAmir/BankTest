package com.springbank.transaction.write.messaging;

import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.transaction.write.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    public static final String EXCHANGE = "banking.exchange";
    public static final String ROUTING_KEY = "transaction.completed";

    public void publishTransactionCompleted(Transaction tx) {
        var event = TransactionCompletedEvent.builder()
                .transactionId(tx.getId())
                .trackingCode(tx.getTrackingCode())
                .fromAccountId(tx.getFromAccountId())
                .toAccountId(tx.getToAccountId())
                .cardId(tx.getCardId())
                .userId(null) // Will be enriched or derived
                .amount(tx.getAmount())
                .type(tx.getType().name())
                .status(tx.getStatus().name())
                .spendingCategory(tx.getSpendingCategory())
                .ipAddress(tx.getIpAddress())
                .deviceFingerprint(tx.getDeviceFingerprint())
                .location(tx.getLocation())
                .timestamp(tx.getCreatedAt())
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
        log.debug("Published transaction.completed event for tx: {}", tx.getId());
    }
}
