package com.springbank.user.mapper;

import com.springbank.user.dto.RoleCreateDto;
import com.springbank.user.dto.RoleResponseDto;
import com.springbank.user.dto.RoleUpdateDto;
import com.springbank.user.entity.Permission;
import com.springbank.user.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoleMapper {

    @Mapping(target = "permissions",
            expression = "java(mapPermissions(role.getPermissions()))")
    RoleResponseDto toResponseDto(Role role);

    Role toEntity(RoleCreateDto dto);

    void updateRoleFromDto(RoleUpdateDto dto, @MappingTarget Role role);

    default Set<String> mapPermissions(Set<Permission> permissions) {
        if (permissions == null) return Set.of();
        return permissions.stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }
}