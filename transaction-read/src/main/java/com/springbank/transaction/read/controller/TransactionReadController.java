package com.springbank.transaction.read.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.transaction.read.dto.response.TransactionResponseDto;
import com.springbank.transaction.read.service.TransactionReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.springbank.transaction.read.service.TransactionExportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionReadController {

    private final TransactionReadService transactionReadService;
    private final TransactionExportService transactionExportService;
    private final com.springbank.transaction.read.mapper.TransactionMapper transactionMapper;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Pageable pageable) {
        Page<TransactionResponseDto> page = transactionExportService.searchTransactions(userId, accountId, status, type, start, end, pageable)
                .map(transactionMapper::toDto);
        return ResponseEntity.ok(ApiResponse.success("Search results", page, "/api/transactions/search"));
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        InputStreamResource file = new InputStreamResource(transactionExportService.exportToExcel(userId, accountId, status, type, start, end));
        String filename = "Transactions_" + LocalDateTime.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Transaction found", transactionReadService.getById(id), "/api/transactions/" + id));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<TransactionResponseDto>>> getByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success("Account transactions", transactionReadService.getByAccountId(accountId), "/api/transactions/account/" + accountId));
    }

    @GetMapping("/card/{cardId}")
    public ResponseEntity<ApiResponse<List<TransactionResponseDto>>> getByCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(ApiResponse.success("Card transactions", transactionReadService.getByCardId(cardId), "/api/transactions/card/" + cardId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<TransactionResponseDto>>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("User transactions", transactionReadService.getByUserId(userId), "/api/transactions/user/" + userId));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<TransactionResponseDto>>> getRecent() {
        return ResponseEntity.ok(ApiResponse.success("Recent transactions", transactionReadService.getRecentTransactions(), "/api/transactions/recent"));
    }

    @GetMapping("/tracking/{trackingCode}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getByTracking(@PathVariable String trackingCode) {
        return ResponseEntity.ok(ApiResponse.success("Transaction found", transactionReadService.getByTrackingCode(trackingCode), "/api/transactions/tracking/" + trackingCode));
    }
}
