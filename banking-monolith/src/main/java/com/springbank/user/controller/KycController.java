package com.springbank.user.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.common.enums.KycStatus;
import com.springbank.user.dto.KycReviewDto;
import com.springbank.user.dto.KycSubmitDto;
import com.springbank.user.dto.KycVerificationDto;
import com.springbank.user.service.KycReadService;
import com.springbank.user.service.KycWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC Verification", description = "احراز هویت تکمیلی (KYC) - Basic, Standard, Enhanced")
@SecurityRequirement(name = "bearerAuth")
public class KycController {

    private final KycReadService kycReadService;
    private final KycWriteService kycWriteService;

    @Operation(summary = "دریافت KYC بر اساس userId")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<KycVerificationDto>> getByUserId(@PathVariable Long userId) {
        log.info("[KYC-API] GET /api/kyc/user/{}", userId);
        KycVerificationDto dto = kycReadService.getByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("KYC loaded successfully", dto, "/api/kyc/user/" + userId));
    }

    @Operation(summary = "دریافت KYC بر اساس ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KycVerificationDto>> getById(@PathVariable Long id) {
        log.info("[KYC-API] GET /api/kyc/{}", id);
        KycVerificationDto dto = kycReadService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("KYC loaded successfully", dto, "/api/kyc/" + id));
    }

    @Operation(summary = "لیست KYC بر اساس وضعیت (Admin)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<KycVerificationDto>>> listByStatus(@RequestParam(required = false) KycStatus status) {
        log.info("[KYC-API] GET /api/kyc?status={}", status);
        List<KycVerificationDto> list = status != null ? kycReadService.findByStatus(status) : kycReadService.findAll();
        return ResponseEntity.ok(ApiResponse.success("KYC list loaded", list, "/api/kyc"));
    }

    @Operation(summary = "ثبت درخواست KYC (ارسال مدارک)")
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<KycVerificationDto>> submit(@RequestBody KycSubmitDto dto) {
        log.info("[KYC-API] POST /api/kyc/submit userId={}", dto.userId());
        KycVerificationDto result = kycWriteService.submitKyc(dto);
        return ResponseEntity.ok(ApiResponse.success("KYC submitted successfully", result, "/api/kyc/submit"));
    }

    @Operation(summary = "بررسی و تأیید/رد KYC (Admin/Employee)")
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ResponseEntity<ApiResponse<KycVerificationDto>> review(@PathVariable Long id,
                                                                  @RequestBody KycReviewDto dto,
                                                                  Authentication auth) {
        String reviewer = auth.getName();
        log.info("[KYC-API] POST /api/kyc/{}/review by reviewer={}", id, reviewer);
        KycVerificationDto result = kycWriteService.reviewKyc(id, dto, reviewer);
        return ResponseEntity.ok(ApiResponse.success("KYC reviewed successfully", result, "/api/kyc/" + id + "/review"));
    }
}
