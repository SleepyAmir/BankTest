package com.springbank.user.mapper;

import com.springbank.user.dto.RoleCreateDto;
import com.springbank.user.dto.RoleResponseDto;
import com.springbank.user.dto.RoleUpdateDto;
import com.springbank.user.entity.Role;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-11T00:37:16+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class RoleMapperImpl implements RoleMapper {

    @Override
    public RoleResponseDto toResponseDto(Role role) {
        if ( role == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        String description = null;
        int priority = 0;

        id = role.getId();
        name = role.getName();
        description = role.getDescription();
        priority = role.getPriority();

        Set<String> permissions = mapPermissions(role.getPermissions());

        RoleResponseDto roleResponseDto = new RoleResponseDto( id, name, description, priority, permissions );

        return roleResponseDto;
    }

    @Override
    public Role toEntity(RoleCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        Role.RoleBuilder<?, ?> role = Role.builder();

        role.name( dto.name() );
        role.description( dto.description() );
        role.priority( dto.priority() );

        return role.build();
    }

    @Override
    public void updateRoleFromDto(RoleUpdateDto dto, Role role) {
        if ( dto == null ) {
            return;
        }

        if ( dto.name() != null ) {
            role.setName( dto.name() );
        }
        if ( dto.description() != null ) {
            role.setDescription( dto.description() );
        }
        role.setPriority( dto.priority() );
    }
}
