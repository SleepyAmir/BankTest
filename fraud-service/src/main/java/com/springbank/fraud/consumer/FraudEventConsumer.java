package com.springbank.fraud.consumer;

import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.fraud.service.FraudAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudEventConsumer {

    private final FraudAnalysisService fraudAnalysisService;

    @RabbitListener(queues = "fraud.queue")
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("Fraud service received transaction completed event: {}", event.getTransactionId());
        fraudAnalysisService.analyzeTransaction(event);
    }
}
