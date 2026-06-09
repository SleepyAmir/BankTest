package com.springbank.user.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.common.enums.KycLevel;
import com.springbank.common.enums.KycStatus;
import com.springbank.user.dto.KycReviewDto;
import com.springbank.user.dto.KycSubmitDto;
import com.springbank.user.dto.KycVerificationDto;
import com.springbank.user.service.KycReadService;
import com.springbank.user.service.KycWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ============================================================================
 * KYC CONTROLLER — احراز هویت تکمیلی (فلوی ۲)
 * ============================================================================
 *  - submit/upload: کاربر مدارک را بارگذاری می‌کند → DOCUMENT_UPLOADED
 *  - start-review / review: فقط ROLE_MANAGER و ROLE_ADMIN
 * ============================================================================
 */
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
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @securityUserService.isCurrentUser(#userId, authentication)")
    public ResponseEntity<ApiResponse<KycVerificationDto>> getByUserId(@PathVariable Long userId) {
        KycVerificationDto dto = kycReadService.getByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("KYC loaded successfully", dto, "/api/kyc/user/" + userId));
    }

    @Operation(summary = "دریافت KYC بر اساس ID (Manager/Admin)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<KycVerificationDto>> getById(@PathVariable Long id) {
        KycVerificationDto dto = kycReadService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("KYC loaded successfully", dto, "/api/kyc/" + id));
    }

    @Operation(summary = "لیست KYC بر اساس وضعیت — پنل مدیر/کارمند")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<KycVerificationDto>>> listByStatus(
            @RequestParam(required = false) KycStatus status) {
        List<KycVerificationDto> list = status != null
                ? kycReadService.findByStatus(status)
                : kycReadService.findAll();
        return ResponseEntity.ok(ApiResponse.success("KYC list loaded", list, "/api/kyc"));
    }

    @Operation(summary = "ثبت KYC با مسیر مدارک از قبل آماده (JSON)")
    @PostMapping("/submit")
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#dto.userId(), authentication)")
    public ResponseEntity<ApiResponse<KycVerificationDto>> submit(@Valid @RequestBody KycSubmitDto dto) {
        KycVerificationDto result = kycWriteService.submitKyc(dto);
        return ResponseEntity.ok(ApiResponse.success("KYC submitted successfully", result, "/api/kyc/submit"));
    }

    @Operation(summary = "بارگذاری مدارک KYC (کارت ملی + سلفی) — multipart/form-data")
    @PostMapping(value = "/{userId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or @securityUserService.isCurrentUser(#userId, authentication)")
    public ResponseEntity<ApiResponse<KycVerificationDto>> uploadDocuments(
            @PathVariable Long userId,
            @RequestParam(value = "level", required = false) KycLevel level,
            @RequestPart("nationalId") MultipartFile nationalId,
            @RequestPart("selfie") MultipartFile selfie,
            @RequestPart(value = "addressProof", required = false) MultipartFile addressProof) {
        log.info("[KYC-API] POST /api/kyc/{}/documents", userId);
        KycVerificationDto result = kycWriteService.uploadDocuments(userId, level, nationalId, selfie, addressProof);
        return ResponseEntity.ok(ApiResponse.success("مدارک با موفقیت بارگذاری شد", result,
                "/api/kyc/" + userId + "/documents"));
    }

    @Operation(summary = "شروع بررسی KYC (Manager/Admin) → UNDER_REVIEW")
    @PostMapping("/{id}/start-review")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<KycVerificationDto>> startReview(@PathVariable Long id, Authentication auth) {
        KycVerificationDto result = kycWriteService.startReview(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("بررسی آغاز شد", result, "/api/kyc/" + id + "/start-review"));
    }

    @Operation(summary = "تأیید/رد KYC توسط مدیر شعبه (Manager/Admin)")
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<KycVerificationDto>> review(@PathVariable Long id,
                                                                  @Valid @RequestBody KycReviewDto dto,
                                                                  Authentication auth) {
        KycVerificationDto result = kycWriteService.reviewKyc(id, dto, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("KYC reviewed successfully", result, "/api/kyc/" + id + "/review"));
    }
}
