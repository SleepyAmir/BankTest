package com.springbank.user.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.user.dto.ChangePasswordDto;
import com.springbank.user.dto.UserResponseDto;
import com.springbank.user.dto.UserUpdateDto;
import com.springbank.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> me(@AuthenticationPrincipal UserDetails principal) {
        UserResponseDto dto = userService.getUserByUsername(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Current user", dto, "/api/users/me"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#id, authentication)")
    public ResponseEntity<ApiResponse<UserResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User found", userService.getUserById(id), "/api/users/" + id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All users", userService.getAllUsers().stream().toList(), "/api/users"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#id, authentication)")
    public ResponseEntity<ApiResponse<UserResponseDto>> update(@PathVariable Long id, @RequestBody UserUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("User updated", userService.updateUser(id, dto), "/api/users/" + id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null, "/api/users/" + id));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDto>> assignRoles(@PathVariable Long id, @RequestBody Set<Long> roleIds) {
        return ResponseEntity.ok(ApiResponse.success("Roles assigned", userService.assignRolesToUser(id, roleIds), "/api/users/" + id + "/roles"));
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#id, authentication)")
    public ResponseEntity<ApiResponse<Void>> changePassword(@PathVariable Long id, @RequestBody ChangePasswordDto dto) {
        userService.changePassword(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Password changed", null, "/api/users/" + id + "/change-password"));
    }
}
