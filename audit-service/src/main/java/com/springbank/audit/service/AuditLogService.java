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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Transactional
    public void saveAuditEvent(AuditLogEvent event) {
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

        auditLogRepository.save(logEntry);
        log.debug("Audit log saved: {} on {} id={}", event.getAction(), event.getEntityType(), event.getEntityId());
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getByActor(String actorUsername) {
        return auditLogRepository.findByActorUsernameOrderByTimestampDesc(actorUsername).stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getByAction(String action) {
        return auditLogRepository.findByAction(action).stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetween(start, end).stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAll() {
        return auditLogRepository.findAll().stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
    }
}
