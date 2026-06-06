package com.springbank.user.mapper;

import com.springbank.user.dto.RoleCreateDto;
import com.springbank.user.dto.RoleResponseDto;
import com.springbank.user.dto.RoleUpdateDto;
import com.springbank.user.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoleMapper {
    RoleResponseDto toResponseDto(Role role);
    Role toEntity(RoleCreateDto dto);
    void updateRoleFromDto(RoleUpdateDto dto, @MappingTarget Role role);
}
