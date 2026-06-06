package com.springbank.audit.controller;

import com.springbank.audit.dto.AuditLogDto;
import com.springbank.audit.service.AuditLogService;
import com.springbank.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/actor/{username}")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getByActor(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success("Audit logs by actor", auditLogService.getByActor(username), "/api/audit/actor/" + username));
    }

    @GetMapping("/entity/{type}/{id}")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getByEntity(@PathVariable String type, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Audit logs by entity", auditLogService.getByEntity(type, id), "/api/audit/entity/" + type + "/" + id));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getByAction(@PathVariable String action) {
        return ResponseEntity.ok(ApiResponse.success("Audit logs by action", auditLogService.getByAction(action), "/api/audit/action/" + action));
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(ApiResponse.success("Audit logs by range", auditLogService.getByDateRange(start, end), "/api/audit/range"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All audit logs", auditLogService.getAll(), "/api/audit"));
    }
}
