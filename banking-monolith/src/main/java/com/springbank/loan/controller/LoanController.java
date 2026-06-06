package com.springbank.loan.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.loan.dto.LoanCreateDto;
import com.springbank.loan.dto.LoanInstallmentDto;
import com.springbank.loan.dto.LoanResponseDto;
import com.springbank.loan.dto.LoanUpdateDto;
import com.springbank.loan.service.LoanReadService;
import com.springbank.loan.service.LoanWriteService;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LoanResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All loans", loanReadService.getByUserId(null), "/api/loans"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#loanReadService.getEntityById(#id).user.id, authentication)")
    public ResponseEntity<ApiResponse<LoanResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Loan found", loanReadService.getById(id), "/api/loans/" + id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#userId, authentication)")
    public ResponseEntity<ApiResponse<List<LoanResponseDto>>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("User loans", loanReadService.getByUserId(userId), "/api/loans/user/" + userId));
    }

    @GetMapping("/{id}/installments")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#loanReadService.getEntityById(#id).user.id, authentication)")
    public ResponseEntity<ApiResponse<List<LoanInstallmentDto>>> getInstallments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Loan installments", loanReadService.getInstallmentsByLoanId(id), "/api/loans/" + id + "/installments"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<LoanResponseDto>> create(@RequestBody LoanCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Loan created", loanWriteService.createLoan(dto), "/api/loans"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponseDto>> update(@PathVariable Long id, @RequestBody LoanUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Loan updated", loanWriteService.updateLoan(id, dto), "/api/loans/" + id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponseDto>> approve(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Loan approved", loanWriteService.approveLoan(id, principal.getUsername()), "/api/loans/" + id + "/approve"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponseDto>> reject(@PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success("Loan rejected", loanWriteService.rejectLoan(id, reason), "/api/loans/" + id + "/reject"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        loanWriteService.deleteLoan(id);
        return ResponseEntity.ok(ApiResponse.success("Loan deleted", null, "/api/loans/" + id));
    }
}
