package com.springbank.user.mapper;

import com.springbank.user.dto.PermissionCreateDto;
import com.springbank.user.dto.PermissionResponseDto;
import com.springbank.user.dto.PermissionUpdateDto;
import com.springbank.user.entity.Permission;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-06T19:59:14+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class PermissionMapperImpl implements PermissionMapper {

    @Override
    public PermissionResponseDto toResponseDto(Permission permission) {
        if ( permission == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        String description = null;
        String category = null;
        boolean systemDefault = false;

        id = permission.getId();
        name = permission.getName();
        description = permission.getDescription();
        category = permission.getCategory();
        systemDefault = permission.isSystemDefault();

        PermissionResponseDto permissionResponseDto = new PermissionResponseDto( id, name, description, category, systemDefault );

        return permissionResponseDto;
    }

    @Override
    public Permission toEntity(PermissionCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        Permission.PermissionBuilder<?, ?> permission = Permission.builder();

        permission.name( dto.name() );
        permission.description( dto.description() );
        permission.category( dto.category() );

        return permission.build();
    }

    @Override
    public void updatePermissionFromDto(PermissionUpdateDto dto, Permission permission) {
        if ( dto == null ) {
            return;
        }

        if ( dto.name() != null ) {
            permission.setName( dto.name() );
        }
        if ( dto.description() != null ) {
            permission.setDescription( dto.description() );
        }
        if ( dto.category() != null ) {
            permission.setCategory( dto.category() );
        }
    }
}
