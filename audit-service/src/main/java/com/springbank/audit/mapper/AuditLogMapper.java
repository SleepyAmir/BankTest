package com.springbank.audit.mapper;

import com.springbank.audit.dto.AuditLogDto;
import com.springbank.audit.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AuditLogMapper {
    AuditLogDto toDto(AuditLog entity);
}
