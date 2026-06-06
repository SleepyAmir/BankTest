package com.springbank.audit.consumer;

import com.springbank.common.event.AuditLogEvent;
import com.springbank.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * AUDIT EVENT CONSUMER
 * ============================================================================
 * Listens to: audit.queue (routing: audit.*)
 * Source: @Auditable AOP aspect in Monolith publishes after business actions
 * Action: Persists audit log to DB (port 5436) for compliance tracking
 *
 * LOG MARKERS: [AUDIT-RECV] [AUDIT-SAVE] [AUDIT-ERR]
 * ============================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditLogService auditLogService;

    @RabbitListener(queues = "audit.queue")
    public void handleAuditLogEvent(AuditLogEvent event) {
        log.info("[AUDIT-RECV] ==================== AUDIT LOG ====================");
        log.info("[AUDIT-RECV] Received AuditLogEvent: action={}, entity={}, entityId={}, actor={}",
                event.getAction(), event.getEntityType(), event.getEntityId(), event.getActorUsername());

        try {
            auditLogService.saveAuditEvent(event);
            log.info("[AUDIT-SAVE] ✅ Audit log saved for {} action on {} id={}",
                    event.getAction(), event.getEntityType(), event.getEntityId());
            log.info("[AUDIT-SAVE] ✅ Query via GET /api/audit/actor/{} or /api/audit/entity/{}/{}",
                    event.getActorUsername(), event.getEntityType(), event.getEntityId());
        } catch (Exception e) {
            log.error("[AUDIT-ERR] ❌ Failed to save audit log: {}", e.getMessage());
            log.error("[AUDIT-ERR] ⚠️ Audit trail broken! Check audit DB (port 5436) is running.");
            throw e;
        }
        log.info("[AUDIT-RECV] ==================== AUDIT DONE ====================\n");
    }
}
