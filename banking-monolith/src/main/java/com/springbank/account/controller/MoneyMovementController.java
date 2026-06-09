package com.springbank.account.controller;

import com.springbank.account.dto.DepositRequestDto;
import com.springbank.account.dto.TransactionResultDto;
import com.springbank.account.dto.TransferRequestDto;
import com.springbank.account.service.MoneyMovementService;
import com.springbank.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * MONEY MOVEMENT CONTROLLER — شارژ و انتقال وجه (فلوهای ۴ و ۵)
 * ============================================================================
 * کنترل دسترسی: کاربر فقط می‌تواند از حساب «خودش» شارژ/انتقال انجام دهد
 * (یا ADMIN/MANAGER). مالکیت حساب در سطح SpEL بررسی می‌شود.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Money Movement", description = "شارژ حساب و انتقال وجه داخلی")
@SecurityRequirement(name = "bearerAuth")
public class MoneyMovementController {

    private final MoneyMovementService moneyMovementService;

    @Operation(summary = "شارژ حساب از طریق درگاه (DEPOSIT)")
    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') "
            + "or @securityUserService.isCurrentUser(@accountReadService.getById(#dto.accountId()).userId(), authentication)")
    public ResponseEntity<ApiResponse<TransactionResultDto>> deposit(@Valid @RequestBody DepositRequestDto dto) {
        TransactionResultDto result = moneyMovementService.deposit(dto);
        return ResponseEntity.ok(ApiResponse.success("شارژ حساب با موفقیت انجام شد", result, "/api/transactions/deposit"));
    }

    @Operation(summary = "انتقال وجه داخلی بین دو حساب (TRANSFER)")
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') "
            + "or @securityUserService.isCurrentUser(@accountReadService.getById(#dto.fromAccountId()).userId(), authentication)")
    public ResponseEntity<ApiResponse<TransactionResultDto>> transfer(@Valid @RequestBody TransferRequestDto dto) {
        TransactionResultDto result = moneyMovementService.transfer(dto);
        return ResponseEntity.ok(ApiResponse.success("انتقال وجه با موفقیت انجام شد", result, "/api/transactions/transfer"));
    }
}
