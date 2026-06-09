package com.springbank.user.service;

import com.springbank.common.enums.KycLevel;
import com.springbank.common.enums.KycStatus;
import com.springbank.common.exception.BusinessException;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.common.storage.FileStorageService;
import com.springbank.user.dto.KycReviewDto;
import com.springbank.user.dto.KycSubmitDto;
import com.springbank.user.dto.KycVerificationDto;
import com.springbank.user.entity.KycVerification;
import com.springbank.user.entity.User;
import com.springbank.user.mapper.KycMapper;
import com.springbank.user.repository.KycVerificationRepository;
import com.springbank.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * KYC WRITE SERVICE — مدیریت احراز هویت تکمیلی (فلوی ۲)
 * ============================================================================
 * چرخه‌ی وضعیت:
 *   PENDING ──submit/upload──► DOCUMENT_UPLOADED ──manager opens──► UNDER_REVIEW
 *           ──approve──► APPROVED   |   ──reject──► REJECTED
 *
 * فقط نقش‌های MANAGER/ADMIN مجاز به بررسی هستند (در لایه‌ی کنترلر اعمال می‌شود).
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KycWriteService {

    private final KycVerificationRepository kycRepository;
    private final UserRepository userRepository;
    private final KycMapper kycMapper;
    private final FileStorageService fileStorageService;

    /**
     * ساخت یا به‌روزرسانی رکورد KYC با مسیر مدارک (نسخه‌ی بدون آپلود فایل — مسیرها از قبل آماده‌اند).
     */
    public KycVerificationDto submitKyc(KycSubmitDto dto) {
        log.info("[KYC-SUBMIT] ثبت KYC برای userId={}, requestedLevel={}", dto.userId(), dto.requestedLevel());
        User user = loadUser(dto.userId());

        KycVerification kyc = getOrCreate(user, dto.requestedLevel());
        applyPathsIfPresent(kyc, dto.nationalIdImagePath(), dto.selfieImagePath(), dto.addressProofPath());
        moveToUploadedIfDocsPresent(kyc);

        KycVerification saved = kycRepository.save(kyc);
        log.info("[KYC-SUBMIT] ✅ KYC ثبت شد: id={}, status={}, level={}", saved.getId(), saved.getStatus(), saved.getLevel());
        return kycMapper.toDto(saved);
    }

    /**
     * بارگذاری واقعی مدارک KYC (تصویر کارت ملی و سلفی الزامی، مدرک آدرس اختیاری).
     * پس از ذخیره‌ی فایل‌ها، وضعیت به {@code DOCUMENT_UPLOADED} می‌رود.
     *
     * @param userId       شناسه‌ی کاربر
     * @param requestedLevel سطح درخواستی (اختیاری)
     * @param nationalId   تصویر کارت ملی (الزامی)
     * @param selfie       عکس سلفی (الزامی)
     * @param addressProof مدرک آدرس (اختیاری)
     */
    public KycVerificationDto uploadDocuments(Long userId,
                                              KycLevel requestedLevel,
                                              MultipartFile nationalId,
                                              MultipartFile selfie,
                                              MultipartFile addressProof) {
        log.info("[KYC-UPLOAD] بارگذاری مدارک KYC برای userId={}", userId);
        User user = loadUser(userId);

        if (nationalId == null || nationalId.isEmpty()) {
            throw new BusinessException("تصویر کارت ملی الزامی است");
        }
        if (selfie == null || selfie.isEmpty()) {
            throw new BusinessException("عکس سلفی الزامی است");
        }

        KycVerification kyc = getOrCreate(user, requestedLevel);

        String folder = "user-" + user.getId();
        kyc.setNationalIdImagePath(fileStorageService.store(nationalId, folder, "national-id"));
        kyc.setSelfieImagePath(fileStorageService.store(selfie, folder, "selfie"));
        if (addressProof != null && !addressProof.isEmpty()) {
            kyc.setAddressProofPath(fileStorageService.store(addressProof, folder, "address-proof"));
        }

        kyc.setStatus(KycStatus.DOCUMENT_UPLOADED);
        kyc.setRejectionReason(null);
        kyc.setVerifiedBy(null);
        kyc.setVerifiedAt(null);

        KycVerification saved = kycRepository.save(kyc);
        log.info("[KYC-UPLOAD] ✅ مدارک ذخیره شد و وضعیت به DOCUMENT_UPLOADED تغییر کرد. kycId={}", saved.getId());
        return kycMapper.toDto(saved);
    }

    /**
     * کارمند/مدیر رکورد را برای بررسی باز می‌کند → وضعیت {@code UNDER_REVIEW}.
     */
    public KycVerificationDto startReview(Long id, String reviewerUsername) {
        log.info("[KYC-REVIEW] شروع بررسی KYC id={} توسط {}", id, reviewerUsername);
        KycVerification kyc = loadKyc(id);

        if (kyc.getStatus() != KycStatus.DOCUMENT_UPLOADED && kyc.getStatus() != KycStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "فقط مدارک بارگذاری‌شده قابل بررسی هستند. وضعیت فعلی: " + kyc.getStatus());
        }
        kyc.setStatus(KycStatus.UNDER_REVIEW);
        KycVerification saved = kycRepository.save(kyc);
        return kycMapper.toDto(saved);
    }

    /**
     * تأیید یا رد نهایی KYC توسط مدیر شعبه.
     */
    public KycVerificationDto reviewKyc(Long id, KycReviewDto dto, String reviewerUsername) {
        log.info("[KYC-REVIEW] تصمیم نهایی KYC id={} توسط {}: {}", id, reviewerUsername, dto.status());
        KycVerification kyc = loadKyc(id);

        if (kyc.getStatus() == KycStatus.PENDING) {
            throw new IllegalStateException("کاربر هنوز مدارک خود را بارگذاری نکرده است");
        }
        if (dto.status() != KycStatus.APPROVED && dto.status() != KycStatus.REJECTED) {
            throw new BusinessException("نتیجه‌ی بررسی باید APPROVED یا REJECTED باشد");
        }
        if (dto.status() == KycStatus.REJECTED
                && (dto.rejectionReason() == null || dto.rejectionReason().isBlank())) {
            throw new BusinessException("برای رد درخواست، ذکر دلیل الزامی است");
        }

        kyc.setStatus(dto.status());
        if (dto.status() == KycStatus.APPROVED) {
            if (dto.approvedLevel() != null) {
                kyc.setLevel(dto.approvedLevel());
            }
            kyc.setRejectionReason(null);
            kyc.setVerifiedAt(LocalDateTime.now());
        } else {
            kyc.setRejectionReason(dto.rejectionReason());
            kyc.setVerifiedAt(null);
        }
        kyc.setVerifiedBy(reviewerUsername);

        KycVerification saved = kycRepository.save(kyc);
        log.info("[KYC-REVIEW] ✅ KYC id={} بررسی شد. status={}, level={}, by={}",
                saved.getId(), saved.getStatus(), saved.getLevel(), reviewerUsername);
        return kycMapper.toDto(saved);
    }

    // ===================== Helpers =====================

    private User loadUser(Long userId) {
        return userRepository.findActiveById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private KycVerification loadKyc(Long id) {
        return kycRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KYC Verification", id));
    }

    private KycVerification getOrCreate(User user, KycLevel requestedLevel) {
        return kycRepository.findByUserId(user.getId())
                .map(existing -> {
                    if (requestedLevel != null) existing.setLevel(requestedLevel);
                    return existing;
                })
                .orElseGet(() -> KycVerification.builder()
                        .user(user)
                        .status(KycStatus.PENDING)
                        .level(requestedLevel != null ? requestedLevel : KycLevel.BASIC)
                        .build());
    }

    private void applyPathsIfPresent(KycVerification kyc, String nationalId, String selfie, String addressProof) {
        if (nationalId != null) kyc.setNationalIdImagePath(nationalId);
        if (selfie != null) kyc.setSelfieImagePath(selfie);
        if (addressProof != null) kyc.setAddressProofPath(addressProof);
    }

    private void moveToUploadedIfDocsPresent(KycVerification kyc) {
        if (kyc.getNationalIdImagePath() != null && kyc.getSelfieImagePath() != null
                && kyc.getStatus() == KycStatus.PENDING) {
            kyc.setStatus(KycStatus.DOCUMENT_UPLOADED);
        }
    }
}
