package com.springbank.transaction.write.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.transaction.write.dto.TransactionResponseDto;
import com.springbank.transaction.write.dto.request.TransactionCreateDto;
import com.springbank.transaction.write.service.TransactionWriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * TRANSACTION WRITE CONTROLLER (CQRS Write)
 * ============================================================================
 * ایجاد تراکنش به‌صورت «اتمیک و یک‌مرحله‌ای» انجام می‌شود (دیگر مرحله‌ی complete جدا
 * وجود ندارد، چون جابجایی پول در همان لحظه و اتمیک انجام می‌شود).
 * ============================================================================
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionWriteController {

    private final TransactionWriteService transactionWriteService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponseDto>> create(@Valid @RequestBody TransactionCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Transaction created", transactionWriteService.createTransaction(dto), "/api/transactions"));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> reverse(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Transaction reversed", transactionWriteService.reverseTransaction(id), "/api/transactions/" + id + "/reverse"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Transaction found", transactionWriteService.getById(id), "/api/transactions/" + id));
    }
}
