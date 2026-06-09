package com.springbank.fraud.messaging;

import com.springbank.common.event.FraudDetectedEvent;
import com.springbank.common.event.TransactionBlockedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * انتشار رویدادهای fraud به RabbitMQ (بدون تغییر در پیکربندی موجود).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE = "banking.exchange";
    public static final String ROUTING_BLOCKED = "transaction.blocked";
    public static final String ROUTING_FRAUD_DETECTED = "fraud.detected";

    /** اعلام بلاک‌شدن تراکنش تا مصرف‌کننده‌ها وضعیت را BLOCKED کنند و به کاربر اطلاع دهند. */
    public void publishBlocked(TransactionBlockedEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_BLOCKED, event);
            log.warn("[FRAUD-PUB] 🚫 TransactionBlockedEvent منتشر شد (trackingCode={})", event.getTrackingCode());
        } catch (Exception e) {
            log.error("[FRAUD-PUB] ❌ انتشار TransactionBlockedEvent ناموفق بود: {}", e.getMessage());
        }
    }

    /** اعلام تشخیص ریسک تقلب (برای نوتیفیکیشن به کاربر). */
    public void publishFraudDetected(FraudDetectedEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_FRAUD_DETECTED, event);
            log.info("[FRAUD-PUB] ⚠️ FraudDetectedEvent منتشر شد (userId={}, level={})",
                    event.getUserId(), event.getRiskLevel());
        } catch (Exception e) {
            log.error("[FRAUD-PUB] ❌ انتشار FraudDetectedEvent ناموفق بود: {}", e.getMessage());
        }
    }
}
