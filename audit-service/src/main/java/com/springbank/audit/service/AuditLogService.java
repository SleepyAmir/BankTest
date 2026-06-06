package com.springbank.audit.service;

import com.springbank.audit.dto.AuditLogDto;
import com.springbank.audit.entity.AuditLog;
import com.springbank.audit.mapper.AuditLogMapper;
import com.springbank.audit.repository.AuditLogRepository;
import com.springbank.common.event.AuditLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * AUDIT LOG SERVICE — Compliance & Audit Trail
 * ============================================================================
 * Triggered by: RabbitMQ event from Monolith @Auditable AOP aspect
 * Action: Persists immutable audit record for every business action
 *
 * LOG MARKERS: [AUDIT-SAVE] [AUDIT-QUERY]
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Transactional
    public void saveAuditEvent(AuditLogEvent event) {
        log.info("[AUDIT-SAVE] Persisting audit: action={}, entity={}, entityId={}, actor={}",
                event.getAction(), event.getEntityType(), event.getEntityId(), event.getActorUsername());

        try {
            AuditLog logEntry = AuditLog.builder()
                    .actorUsername(event.getActorUsername())
                    .action(event.getAction())
                    .entityType(event.getEntityType())
                    .entityId(event.getEntityId())
                    .oldValue(event.getOldValue())
                    .newValue(event.getNewValue())
                    .ipAddress(event.getIpAddress())
                    .timestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                    .reason(event.getReason())
                    .build();

            AuditLog saved = auditLogRepository.save(logEntry);
            log.info("[AUDIT-SAVE] ✅ Audit log saved: id={}, action={}, entity={}, actor={}",
                    saved.getId(), saved.getAction(), saved.getEntityType(), saved.getActorUsername());
        } catch (Exception e) {
            log.error("[AUDIT-SAVE] ❌ FAILED to save audit log: {}", e.getMessage());
            log.error("[AUDIT-SAVE] ⚠️ Audit trail broken! Check audit DB (port 5436) and table structure.");
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getByActor(String actorUsername) {
        log.info("[AUDIT-QUERY] Querying audit logs by actor: {}", actorUsername);
        List<AuditLogDto> result = auditLogRepository.findByActorUsernameOrderByTimestampDesc(actorUsername).stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
        log.info("[AUDIT-QUERY] Found {} audit logs for actor={}", result.size(), actorUsername);
        return result;
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getByEntity(String entityType, Long entityId) {
        log.info("[AUDIT-QUERY] Querying audit logs: entity={} id={}", entityType, entityId);
        List<AuditLogDto> result = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
        log.info("[AUDIT-QUERY] Found {} audit logs for {} id={}", result.size(), entityType, entityId);
        return result;
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getByAction(String action) {
        log.info("[AUDIT-QUERY] Querying audit logs by action: {}", action);
        List<AuditLogDto> result = auditLogRepository.findByAction(action).stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
        log.info("[AUDIT-QUERY] Found {} audit logs for action={}", result.size(), action);
        return result;
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getByDateRange(LocalDateTime start, LocalDateTime end) {
        log.info("[AUDIT-QUERY] Querying audit logs from {} to {}", start, end);
        List<AuditLogDto> result = auditLogRepository.findByTimestampBetween(start, end).stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
        log.info("[AUDIT-QUERY] Found {} audit logs in date range", result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAll() {
        log.info("[AUDIT-QUERY] Querying all audit logs");
        List<AuditLogDto> result = auditLogRepository.findAll().stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
        log.info("[AUDIT-QUERY] Found {} total audit logs", result.size());
        return result;
    }
}
