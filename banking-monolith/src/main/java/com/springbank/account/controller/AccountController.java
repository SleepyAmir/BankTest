package com.springbank.account.controller;

import com.springbank.account.dto.AccountBalanceDto;
import com.springbank.account.dto.AccountCreateDto;
import com.springbank.account.dto.AccountResponseDto;
import com.springbank.account.dto.AccountUpdateDto;
import com.springbank.account.dto.AccountWithCardDto;
import com.springbank.account.dto.OpenAccountDto;
import com.springbank.account.service.AccountReadService;
import com.springbank.account.service.AccountWriteService;
import com.springbank.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountReadService accountReadService;
    private final AccountWriteService accountWriteService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AccountResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All accounts", accountReadService.getAllActive(), "/api/accounts"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(@accountReadService.getById(#id).userId(), authentication)")
    public ResponseEntity<ApiResponse<AccountResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Account found", accountReadService.getById(id), "/api/accounts/" + id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#userId, authentication)")
    public ResponseEntity<ApiResponse<List<AccountResponseDto>>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("User accounts", accountReadService.getByUserId(userId), "/api/accounts/user/" + userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    public ResponseEntity<ApiResponse<AccountResponseDto>> create(@Valid @RequestBody AccountCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Account created", accountWriteService.createAccount(dto), "/api/accounts"));
    }

    @PostMapping("/open")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @securityUserService.isCurrentUser(#dto.userId(), authentication)")
    public ResponseEntity<ApiResponse<AccountWithCardDto>> open(@Valid @RequestBody OpenAccountDto dto) {
        AccountWithCardDto result = accountWriteService.openAccount(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("حساب با موفقیت افتتاح و کارت صادر شد", result, "/api/accounts/open"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(@accountReadService.getById(#id).userId(), authentication)")
    public ResponseEntity<ApiResponse<AccountResponseDto>> update(@PathVariable Long id, @RequestBody AccountUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Account updated", accountWriteService.updateAccount(id, dto), "/api/accounts/" + id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        accountWriteService.deleteAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Account deleted", null, "/api/accounts/" + id));
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(@accountReadService.getById(#id).userId(), authentication)")
    public ResponseEntity<ApiResponse<AccountResponseDto>> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success("Deposit successful", accountWriteService.deposit(id, amount), "/api/accounts/" + id + "/deposit"));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(@accountReadService.getById(#id).userId(), authentication)")
    public ResponseEntity<ApiResponse<AccountResponseDto>> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success("Withdrawal successful", accountWriteService.withdraw(id, amount), "/api/accounts/" + id + "/withdraw"));
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(@accountReadService.getById(#id).userId(), authentication)")
    public ResponseEntity<ApiResponse<AccountBalanceDto>> getBalance(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved", accountReadService.getBalanceInternal(id), "/api/accounts/" + id + "/balance"));
    }
}
