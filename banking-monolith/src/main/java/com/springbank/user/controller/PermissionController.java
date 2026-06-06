package com.springbank.user.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.user.dto.PermissionCreateDto;
import com.springbank.user.dto.PermissionResponseDto;
import com.springbank.user.dto.PermissionUpdateDto;
import com.springbank.user.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PermissionResponseDto>> create(@RequestBody PermissionCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Permission created", permissionService.createPermission(dto), "/api/permissions"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Set<PermissionResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All permissions", permissionService.getAllPermissions(), "/api/permissions"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PermissionResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Permission found", permissionService.getPermissionById(id), "/api/permissions/" + id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PermissionResponseDto>> update(@PathVariable Long id, @RequestBody PermissionUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Permission updated", permissionService.updatePermission(id, dto), "/api/permissions/" + id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.ok(ApiResponse.success("Permission deleted", null, "/api/permissions/" + id));
    }
}
