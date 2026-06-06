package com.springbank.card.controller;

import com.springbank.card.dto.CardCreateDto;
import com.springbank.card.dto.CardResponseDto;
import com.springbank.card.dto.CardUpdateDto;
import com.springbank.card.service.CardReadService;
import com.springbank.card.service.CardWriteService;
import com.springbank.common.dto.ApiResponse;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CardResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All cards", cardReadService.getAllActive(), "/api/cards"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#cardReadService.getEntityById(#id).account.user.id, authentication)")
    public ResponseEntity<ApiResponse<CardResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Card found", cardReadService.getById(id), "/api/cards/" + id));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#accountId, authentication)")
    public ResponseEntity<ApiResponse<List<CardResponseDto>>> getByAccountId(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success("Account cards", cardReadService.getByAccountId(accountId), "/api/cards/account/" + accountId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    public ResponseEntity<ApiResponse<CardResponseDto>> create(@RequestBody CardCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Card created", cardWriteService.createCard(dto), "/api/cards"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#cardReadService.getEntityById(#id).account.user.id, authentication)")
    public ResponseEntity<ApiResponse<CardResponseDto>> update(@PathVariable Long id, @RequestBody CardUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Card updated", cardWriteService.updateCard(id, dto), "/api/cards/" + id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        cardWriteService.deleteCard(id);
        return ResponseEntity.ok(ApiResponse.success("Card deleted", null, "/api/cards/" + id));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#cardReadService.getEntityById(#id).account.user.id, authentication)")
    public ResponseEntity<ApiResponse<CardResponseDto>> block(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Card blocked", cardWriteService.blockCard(id), "/api/cards/" + id + "/block"));
    }
}
