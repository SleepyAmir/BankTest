package com.springbank.audit.consumer;

import com.springbank.common.event.AuditLogEvent;
import com.springbank.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditLogService auditLogService;

    @RabbitListener(queues = "audit.queue")
    public void handleAuditLogEvent(AuditLogEvent event) {
        log.info("Audit service received event: {} on {}", event.getAction(), event.getEntityType());
        auditLogService.saveAuditEvent(event);
    }
}
