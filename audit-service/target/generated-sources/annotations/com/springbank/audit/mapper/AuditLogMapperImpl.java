package com.springbank.audit.mapper;

import com.springbank.audit.dto.AuditLogDto;
import com.springbank.audit.entity.AuditLog;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-06T19:41:52+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class AuditLogMapperImpl implements AuditLogMapper {

    @Override
    public AuditLogDto toDto(AuditLog entity) {
        if ( entity == null ) {
            return null;
        }

        Long id = null;
        String actorUsername = null;
        String action = null;
        String entityType = null;
        Long entityId = null;
        String oldValue = null;
        String newValue = null;
        String ipAddress = null;
        LocalDateTime timestamp = null;
        String reason = null;

        id = entity.getId();
        actorUsername = entity.getActorUsername();
        action = entity.getAction();
        entityType = entity.getEntityType();
        entityId = entity.getEntityId();
        oldValue = entity.getOldValue();
        newValue = entity.getNewValue();
        ipAddress = entity.getIpAddress();
        timestamp = entity.getTimestamp();
        reason = entity.getReason();

        AuditLogDto auditLogDto = new AuditLogDto( id, actorUsername, action, entityType, entityId, oldValue, newValue, ipAddress, timestamp, reason );

        return auditLogDto;
    }
}
