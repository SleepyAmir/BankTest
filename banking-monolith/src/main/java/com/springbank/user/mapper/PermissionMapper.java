package com.springbank.user.mapper;

import com.springbank.user.dto.PermissionCreateDto;
import com.springbank.user.dto.PermissionResponseDto;
import com.springbank.user.dto.PermissionUpdateDto;
import com.springbank.user.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PermissionMapper {
    PermissionResponseDto toResponseDto(Permission permission);
    Permission toEntity(PermissionCreateDto dto);
    void updatePermissionFromDto(PermissionUpdateDto dto, @MappingTarget Permission permission);
}
