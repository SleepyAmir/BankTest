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

import com.springbank.audit.service.AuditLogExportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AuditLogExportService auditLogExportService;
    private final com.springbank.audit.mapper.AuditLogMapper auditLogMapper;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> search(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Pageable pageable) {
        Page<AuditLogDto> page = auditLogExportService.searchLogs(username, action, entityType, start, end, pageable)
                .map(auditLogMapper::toDto);
        return ResponseEntity.ok(ApiResponse.success("Search results", page, "/api/audit/search"));
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        InputStreamResource file = new InputStreamResource(auditLogExportService.exportToExcel(username, action, entityType, start, end));
        String filename = "AuditLogs_" + LocalDateTime.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

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
