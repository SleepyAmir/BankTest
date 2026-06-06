package com.springbank.audit.dto;

import java.time.LocalDateTime;

public record AuditLogDto(
    Long id,
    String actorUsername,
    String action,
    String entityType,
    Long entityId,
    String oldValue,
    String newValue,
    String ipAddress,
    LocalDateTime timestamp,
    String reason
) {}
