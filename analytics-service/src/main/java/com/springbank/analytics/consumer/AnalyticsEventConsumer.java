package com.springbank.analytics.consumer;

import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventConsumer {

    private final AnalyticsService analyticsService;

    @RabbitListener(queues = "analytics.queue")
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("Analytics service received transaction completed event: {}", event.getTransactionId());
        analyticsService.processTransactionEvent(event);
    }
}
