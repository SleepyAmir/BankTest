package com.springbank.card.controller;

import com.springbank.card.dto.CardCreateDto;
import com.springbank.card.dto.CardResponseDto;
import com.springbank.card.dto.CardUpdateDto;
import com.springbank.card.service.CardReadService;
import com.springbank.card.service.CardWriteService;
import com.springbank.common.dto.ApiResponse;
import com.springbank.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardReadService cardReadService;
    private final CardWriteService cardWriteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<CardResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All cards", cardReadService.getAllActive(), "/api/cards"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CardResponseDto>> getById(@PathVariable Long id) {
        CardResponseDto card = cardReadService.getById(id);
        SecurityUtils.requireOwnerOrStaff(card.userId());
        return ResponseEntity.ok(ApiResponse.success("Card found", card, "/api/cards/" + id));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CardResponseDto>>> getByAccountId(@PathVariable Long accountId) {
        // کنترل مالکیت بر اساس صاحب حساب (نه مقایسه‌ی نادرست accountId با userId)
        SecurityUtils.requireOwnerOrStaff(cardReadService.getAccountOwnerUserId(accountId));
        return ResponseEntity.ok(ApiResponse.success("Account cards",
                cardReadService.getByAccountId(accountId), "/api/cards/account/" + accountId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CardResponseDto>> create(@RequestBody CardCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Card created", cardWriteService.createCard(dto), "/api/cards"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CardResponseDto>> update(@PathVariable Long id, @RequestBody CardUpdateDto dto) {
        SecurityUtils.requireOwnerOrStaff(cardReadService.getById(id).userId());
        return ResponseEntity.ok(ApiResponse.success("Card updated", cardWriteService.updateCard(id, dto), "/api/cards/" + id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        cardWriteService.deleteCard(id);
        return ResponseEntity.ok(ApiResponse.success("Card deleted", null, "/api/cards/" + id));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CardResponseDto>> block(@PathVariable Long id) {
        SecurityUtils.requireOwnerOrStaff(cardReadService.getById(id).userId());
        return ResponseEntity.ok(ApiResponse.success("Card blocked", cardWriteService.blockCard(id), "/api/cards/" + id + "/block"));
    }
}
