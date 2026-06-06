package com.springbank.account.controller;

import com.springbank.account.dto.AccountBalanceDto;
import com.springbank.account.dto.AccountCreateDto;
import com.springbank.account.dto.AccountResponseDto;
import com.springbank.account.dto.AccountUpdateDto;
import com.springbank.account.service.AccountReadService;
import com.springbank.account.service.AccountWriteService;
import com.springbank.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
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
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#accountReadService.getEntityById(#id).user.id, authentication)")
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
    public ResponseEntity<ApiResponse<AccountResponseDto>> create(@RequestBody AccountCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Account created", accountWriteService.createAccount(dto), "/api/accounts"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#accountReadService.getEntityById(#id).user.id, authentication)")
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
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#accountReadService.getEntityById(#id).user.id, authentication)")
    public ResponseEntity<ApiResponse<AccountResponseDto>> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success("Deposit successful", accountWriteService.deposit(id, amount), "/api/accounts/" + id + "/deposit"));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#accountReadService.getEntityById(#id).user.id, authentication)")
    public ResponseEntity<ApiResponse<AccountResponseDto>> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success("Withdrawal successful", accountWriteService.withdraw(id, amount), "/api/accounts/" + id + "/withdraw"));
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#accountReadService.getEntityById(#id).user.id, authentication)")
    public ResponseEntity<ApiResponse<AccountBalanceDto>> getBalance(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved", accountReadService.getBalanceInternal(id), "/api/accounts/" + id + "/balance"));
    }
}
