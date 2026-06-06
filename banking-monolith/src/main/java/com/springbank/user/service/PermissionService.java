package com.springbank.user.service;

import com.springbank.user.dto.PermissionCreateDto;
import com.springbank.user.dto.PermissionResponseDto;
import com.springbank.user.dto.PermissionUpdateDto;
import com.springbank.user.mapper.PermissionMapper;
import com.springbank.user.entity.Permission;
import com.springbank.user.repository.PermissionRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class PermissionService extends BaseEntityService<Permission, Long, PermissionResponseDto> {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionService(PermissionRepository permissionRepository,
                             PermissionMapper permissionMapper) {
        super(permissionRepository,
                permissionMapper::toResponseDto,
                dto -> {
                    throw new UnsupportedOperationException(
                            "Use createPermission method instead of createFromDto"
                    );
                });
        this.permissionRepository = permissionRepository;
        this.permissionMapper = permissionMapper;
    }

    @Override
    protected String getEntityTypeName() {
        return "Permission";
    }

    // ========== Create Operations ==========

    @Transactional
    public PermissionResponseDto createPermission(@NonNull PermissionCreateDto createDto) {
        log.info("✨ Creating new permission: {}", createDto.name());

        if (permissionRepository.existsByName(createDto.name())) {
            throw new IllegalArgumentException("Permission already exists: " + createDto.name());
        }

        Permission permission = permissionMapper.toEntity(createDto);
        Permission savedPermission = createEntity(permission);
        log.info("✅ Permission created successfully with id: {}", savedPermission.getId());

        return permissionMapper.toResponseDto(savedPermission);
    }

    // ========== Read Operations ==========

    @Transactional(readOnly = true)
    public PermissionResponseDto getPermissionById(@NonNull Long id) {
        log.debug("🔍 Finding permission by id: {}", id);
        return findDtoById(id);
    }

    @Transactional(readOnly = true)
    public PermissionResponseDto getPermissionByName(@NonNull String name) {
        log.debug("🔍 Finding permission by name: {}", name);

        Permission permission = permissionRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + name));

        return permissionMapper.toResponseDto(permission);
    }

    @Transactional(readOnly = true)
    public Set<PermissionResponseDto> getAllPermissions() {
        log.debug("📋 Fetching all active permissions");
        return Set.copyOf(findAllDtos());
    }

    @Transactional(readOnly = true)
    public Set<PermissionResponseDto> getPermissionsByNames(@NonNull Set<String> names) {
        log.debug("🔍 Finding permissions by names: {}", names);

        return permissionRepository.findByNameIn(names).stream()
                .map(permissionMapper::toResponseDto)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<PermissionResponseDto> getPermissionsByRoleId(@NonNull Long roleId) {
        log.debug("🔍 Finding permissions for role id: {}", roleId);

        return permissionRepository.findAllActivePermissionsByRoleId(roleId).stream()
                .map(permissionMapper::toResponseDto)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<PermissionResponseDto> getPermissionsByUserId(@NonNull Long userId) {
        log.debug("🔍 Finding permissions for user id: {}", userId);

        return permissionRepository.findAllActivePermissionsByUserId(userId).stream()
                .map(permissionMapper::toResponseDto)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Permission getPermissionEntityById(@NonNull Long id) {
        return findEntityById(id);
    }

    // ========== Update Operations ==========

    @Transactional
    public PermissionResponseDto updatePermission(@NonNull Long permissionId, @NonNull PermissionUpdateDto updateDto) {
        log.info("✏️ Updating permission with id: {}", permissionId);

        return updateEntityAndReturnDto(permissionId, permission -> permissionMapper.updatePermissionFromDto(updateDto, permission));
    }

    // ========== Delete Operations ==========

    @Transactional
    public void deletePermission(@NonNull Long permissionId) {
        log.info("🗑️ Deleting permission with id: {}", permissionId);

        Permission permission = findEntityById(permissionId);

        if (permission.isSystemDefault()) {
            throw new IllegalStateException(
                    String.format("Cannot delete system default permission: %s", permission.getName()));
        }

        if (permission.getRoles() != null && !permission.getRoles().isEmpty()) {
            log.warn("⚠️ Permission {} is assigned to {} roles. Soft deleting anyway.",
                    permission.getName(), permission.getRoles().size());
        }

        softDelete(permissionId);
        log.info("✅ Permission deleted successfully: {}", permission.getName());
    }

    @Transactional
    public void hardDeletePermission(@NonNull Long permissionId) {
        log.warn("⚠️ Hard deleting permission with id: {}", permissionId);

        Permission permission = findEntityById(permissionId);

        if (permission.isSystemDefault()) {
            throw new IllegalStateException("Cannot delete system default permission");
        }

        hardDelete(permissionId);
        log.warn("🗑️ Permission hard deleted: {}", permission.getName());
    }

    // ========== Bulk Operations ==========

    @Transactional
    public Set<PermissionResponseDto> createPermissionsBulk(@NonNull Set<PermissionCreateDto> createDtos) {
        log.info("✨ Creating {} permissions in bulk", createDtos.size());

        return createDtos.stream()
                .map(this::createPermission)
                .collect(Collectors.toSet());
    }

    // ========== Utility Methods ==========

    @Transactional(readOnly = true)
    public boolean existsByName(@NonNull String name) {
        return permissionRepository.existsByName(name);
    }
}