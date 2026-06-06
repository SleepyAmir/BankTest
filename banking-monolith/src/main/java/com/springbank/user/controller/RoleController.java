package com.springbank.user.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.user.dto.RoleCreateDto;
import com.springbank.user.dto.RoleResponseDto;
import com.springbank.user.dto.RoleUpdateDto;
import com.springbank.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponseDto>> create(@RequestBody RoleCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Role created", roleService.createRole(dto), "/api/roles"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Set<RoleResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All roles", roleService.getAllRoles(), "/api/roles"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Role found", roleService.getRoleById(id), "/api/roles/" + id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponseDto>> update(@PathVariable Long id, @RequestBody RoleUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Role updated", roleService.updateRole(id, dto), "/api/roles/" + id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Role deleted", null, "/api/roles/" + id));
    }

    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponseDto>> assignPermissions(@PathVariable Long id, @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(ApiResponse.success("Permissions assigned", roleService.assignPermissionsToRole(id, permissionIds), "/api/roles/" + id + "/permissions"));
    }
}
