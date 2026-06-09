package com.springbank.fraud.consumer;

import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.fraud.service.FraudAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * FRAUD EVENT CONSUMER
 * ============================================================================
 * Listens to: fraud.queue (routing: transaction.#)
 * فقط TransactionCompletedEvent را پردازش می‌کند؛ سایر رویدادهای transaction.*
 * (مثلاً transaction.blocked که خود fraud منتشر می‌کند) نادیده گرفته می‌شوند تا
 * از حلقه‌ی پردازش جلوگیری شود.
 *
 * LOG MARKERS: [FRAUD-RECV] [FRAUD-PROC] [FRAUD-ERR]
 * ============================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = "fraud.queue")
public class FraudEventConsumer {

    private final FraudAnalysisService fraudAnalysisService;

    @RabbitHandler
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("[FRAUD-RECV] دریافت TransactionCompletedEvent: trackingCode={}, amount={}, userId={}",
                event.getTrackingCode(), event.getAmount(), event.getUserId());
        try {
            fraudAnalysisService.analyzeTransaction(event);
            log.info("[FRAUD-PROC] ✅ تحلیل تقلب کامل شد برای trackingCode={}", event.getTrackingCode());
        } catch (Exception e) {
            log.error("[FRAUD-ERR] ❌ تحلیل تقلب ناموفق بود برای trackingCode={}: {}",
                    event.getTrackingCode(), e.getMessage(), e);
            throw e;
        }
    }

    /** سایر رویدادها (مثل transaction.blocked) نادیده گرفته می‌شوند. */
    @RabbitHandler(isDefault = true)
    public void handleOther(Object event) {
        log.debug("[FRAUD-RECV] رویداد نامرتبط نادیده گرفته شد: {}", event.getClass().getSimpleName());
    }
}
