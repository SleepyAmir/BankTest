package com.springbank.loan.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.loan.dto.LoanCreateDto;
import com.springbank.loan.dto.LoanInstallmentDto;
import com.springbank.loan.dto.LoanResponseDto;
import com.springbank.loan.dto.LoanUpdateDto;
import com.springbank.loan.service.LoanReadService;
import com.springbank.loan.service.LoanWriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanReadService loanReadService;
    private final LoanWriteService loanWriteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<LoanResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All loans", loanReadService.getAll(), "/api/loans"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @securityUserService.isCurrentUser(@loanReadService.getById(#id).userId(), authentication)")
    public ResponseEntity<ApiResponse<LoanResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Loan found", loanReadService.getById(id), "/api/loans/" + id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @securityUserService.isCurrentUser(#userId, authentication)")
    public ResponseEntity<ApiResponse<List<LoanResponseDto>>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("User loans", loanReadService.getByUserId(userId), "/api/loans/user/" + userId));
    }

    @GetMapping("/{id}/installments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @securityUserService.isCurrentUser(@loanReadService.getById(#id).userId(), authentication)")
    public ResponseEntity<ApiResponse<List<LoanInstallmentDto>>> getInstallments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Loan installments", loanReadService.getInstallmentsByLoanId(id), "/api/loans/" + id + "/installments"));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LoanResponseDto>> create(@Valid @RequestBody LoanCreateDto dto) {
        Long targetUserId = com.springbank.common.security.SecurityUtils.resolveTargetUserId(dto.userId());
        LoanCreateDto resolved = new LoanCreateDto(dto.amount(), dto.durationMonths(), dto.purpose(), targetUserId, dto.accountId());
        return ResponseEntity.ok(ApiResponse.success("Loan created", loanWriteService.createLoan(resolved), "/api/loans"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponseDto>> update(@PathVariable Long id, @Valid @RequestBody LoanUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Loan updated", loanWriteService.updateLoan(id, dto), "/api/loans/" + id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<LoanResponseDto>> approve(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Loan approved", loanWriteService.approveLoan(id, principal.getUsername()), "/api/loans/" + id + "/approve"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<LoanResponseDto>> reject(@PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success("Loan rejected", loanWriteService.rejectLoan(id, reason), "/api/loans/" + id + "/reject"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        loanWriteService.deleteLoan(id);
        return ResponseEntity.ok(ApiResponse.success("Loan deleted", null, "/api/loans/" + id));
    }

    @PostMapping("/installments/{installmentId}/pay")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<LoanInstallmentDto>> payInstallment(@PathVariable Long installmentId) {
        LoanInstallmentDto dto = loanWriteService.payInstallment(installmentId);
        return ResponseEntity.ok(ApiResponse.success("Installment paid successfully", dto, "/api/loans/installments/" + installmentId + "/pay"));
    }
}
