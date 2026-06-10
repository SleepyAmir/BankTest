package com.springbank.transaction.read.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.transaction.read.dto.response.TransactionResponseDto;
import com.springbank.transaction.read.service.TransactionReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionReadController {

    private final TransactionReadService transactionReadService;

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
