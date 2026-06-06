package com.springbank.user.service;

import com.springbank.common.service.BaseEntityService;
import com.springbank.user.dto.RoleCreateDto;
import com.springbank.user.dto.RoleResponseDto;
import com.springbank.user.dto.RoleUpdateDto;
import com.springbank.user.mapper.RoleMapper;
import com.springbank.user.entity.Permission;
import com.springbank.user.entity.Role;
import com.springbank.user.repository.PermissionRepository;
import com.springbank.user.repository.RoleRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class RoleService extends BaseEntityService<Role, Long, RoleResponseDto> {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       RoleMapper roleMapper) {
        super(roleRepository,
                roleMapper::toResponseDto,
                dto -> {
                    throw new UnsupportedOperationException(
                            "Use createRole method instead of createFromDto"
                    );
                });
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    protected String getEntityTypeName() {
        return "Role";
    }

    // ========== Create Operations ==========

    @Transactional
    public RoleResponseDto createRole(@NonNull RoleCreateDto createDto) {
        log.info("✨ Creating new role: {}", createDto.name());

        if (roleRepository.existsByName(createDto.name())) {
            throw new IllegalArgumentException("Role already exists: " + createDto.name());
        }

        Role role = roleMapper.toEntity(createDto);
        Role savedRole = createEntity(role);
        log.info("✅ Role created successfully with id: {}", savedRole.getId());

        return enhanceWithUserCount(roleMapper.toResponseDto(savedRole), savedRole.getId());
    }

    // ========== Read Operations ==========

    @Transactional(readOnly = true)
    public RoleResponseDto getRoleById(@NonNull Long id) {
        log.debug("🔍 Finding role by id: {}", id);
        return findDtoById(id);
    }

    @Transactional(readOnly = true)
    public RoleResponseDto getRoleByName(@NonNull String name) {
        log.debug("🔍 Finding role by name: {}", name);

        Role role = roleRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Role not found: " + name));

        return enhanceWithUserCount(roleMapper.toResponseDto(role), role.getId());
    }

    @Transactional(readOnly = true)
    public RoleResponseDto getRoleByNameWithPermissions(@NonNull String name) {
        log.debug("🔍 Finding role with permissions by name: {}", name);

        Role role = roleRepository.findActiveByNameWithPermissions(name)
                .orElseThrow(() -> new RuntimeException("Role not found: " + name));

        return enhanceWithUserCount(roleMapper.toResponseDto(role), role.getId());
    }

    @Transactional(readOnly = true)
    public Set<RoleResponseDto> getAllRoles() {
        log.debug("📋 Fetching all active roles");

        return findAllEntities().stream()
                .map(role -> enhanceWithUserCount(roleMapper.toResponseDto(role), role.getId()))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<RoleResponseDto> getRolesByUserId(@NonNull Long userId) {
        log.debug("🔍 Finding roles for user id: {}", userId);

        return roleRepository.findAllActiveRolesByUserId(userId).stream()
                .map(role -> enhanceWithUserCount(roleMapper.toResponseDto(role), role.getId()))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Role getRoleEntityById(@NonNull Long id) {
        return findEntityById(id);
    }

    // ========== Update Operations ==========

    @Transactional
    public RoleResponseDto updateRole(@NonNull Long roleId, @NonNull RoleUpdateDto updateDto) {
        log.info("✏️ Updating role with id: {}", roleId);

        RoleResponseDto responseDto = updateEntityAndReturnDto(roleId, role -> roleMapper.updateRoleFromDto(updateDto, role));

        return enhanceWithUserCount(responseDto, roleId);
    }

    // ========== Permission Management ==========

    @Transactional
    public RoleResponseDto assignPermissionsToRole(@NonNull Long roleId, @NonNull Set<Long> permissionIds) {
        log.info("🔗 Assigning {} permissions to role {}", permissionIds.size(), roleId);

        Role role = findEntityById(roleId);

        // ✅ اصلاح: استفاده از HashSet به جای Set.copyOf
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));

        if (permissions.size() != permissionIds.size()) {
            Set<Long> foundIds = permissions.stream()
                    .map(Permission::getId)
                    .collect(Collectors.toSet());
            Set<Long> missingIds = permissionIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new IllegalArgumentException("Some permission IDs are invalid: " + missingIds);
        }

        role.setPermissions(permissions);
        Role savedRole = repository.save(role);

        log.info("✅ Permissions assigned successfully to role: {}", role.getName());

        return enhanceWithUserCount(roleMapper.toResponseDto(savedRole), roleId);
    }

    @Transactional
    public RoleResponseDto addPermissionToRole(@NonNull Long roleId, @NonNull Long permissionId) {
        log.info("🔗 Adding permission {} to role {}", permissionId, roleId);

        Role role = findEntityById(roleId);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found with id: " + permissionId));

        role.getPermissions().add(permission);
        Role savedRole = repository.save(role);

        log.debug("✅ Permission {} added to role {}", permission.getName(), role.getName());

        return enhanceWithUserCount(roleMapper.toResponseDto(savedRole), roleId);
    }

    @Transactional
    public RoleResponseDto removePermissionFromRole(@NonNull Long roleId, @NonNull Long permissionId) {
        log.info("🔗 Removing permission {} from role {}", permissionId, roleId);

        Role role = findEntityById(roleId);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found with id: " + permissionId));

        role.getPermissions().remove(permission);
        Role savedRole = repository.save(role);

        log.debug("✅ Permission {} removed from role {}", permission.getName(), role.getName());

        return enhanceWithUserCount(roleMapper.toResponseDto(savedRole), roleId);
    }

    // ========== Delete Operations ==========

    @Transactional
    public void deleteRole(@NonNull Long roleId) {
        log.info("🗑️ Deleting role with id: {}", roleId);

        Role role = findEntityById(roleId);

        long userCount = roleRepository.countActiveUsersByRoleId(roleId);
        if (userCount > 0) {
            throw new IllegalStateException(
                    String.format("Cannot delete role '%s' as it is assigned to %d active user(s)",
                            role.getName(), userCount));
        }

        if ("ROLE_USER".equals(role.getName()) || "ROLE_ADMIN".equals(role.getName())) {
            throw new IllegalStateException(
                    String.format("Cannot delete system role: %s", role.getName()));
        }

        softDelete(roleId);
        log.info("✅ Role deleted successfully: {}", role.getName());
    }

    // ========== Utility Methods ==========

    @Transactional(readOnly = true)
    public boolean roleExists(@NonNull String name) {
        return roleRepository.existsByName(name);
    }

    @Transactional(readOnly = true)
    public long countUsersByRoleId(@NonNull Long roleId) {
        return roleRepository.countActiveUsersByRoleId(roleId);
    }

    // ========== Helper Methods ==========

    private RoleResponseDto enhanceWithUserCount(RoleResponseDto dto, @NonNull Long roleId) {
        long userCount = roleRepository.countActiveUsersByRoleId(roleId);
        return dto;
    }
}