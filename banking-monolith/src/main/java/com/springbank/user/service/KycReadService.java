package com.springbank.user.service;

import com.springbank.common.enums.KycStatus;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.user.dto.KycVerificationDto;
import com.springbank.user.entity.KycVerification;
import com.springbank.user.mapper.KycMapper;
import com.springbank.user.repository.KycVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KycReadService {

    private final KycVerificationRepository kycRepository;
    private final KycMapper kycMapper;

    /** مسیر نسبی فایل مدرک یک KYC بر اساس نوع ("national" | "selfie" | "address"). */
    public String getDocumentPath(Long kycId, String docType) {
        KycVerification kyc = kycRepository.findActiveById(kycId)
                .orElseThrow(() -> new com.springbank.common.exception.ResourceNotFoundException("KYC Verification", kycId));
        return switch (docType) {
            case "national" -> kyc.getNationalIdImagePath();
            case "selfie" -> kyc.getSelfieImagePath();
            case "address" -> kyc.getAddressProofPath();
            default -> null;
        };
    }

    public KycVerificationDto getById(Long id) {
        log.info("[KYC-READ] Fetching KYC id={}", id);
        KycVerification kyc = kycRepository.findActiveById(id)
                .orElseThrow(() -> {
                    log.error("[KYC-READ] ❌ KYC not found id={}", id);
                    return new ResourceNotFoundException("KYC Verification", id);
                });
        log.info("[KYC-READ] ✅ Found KYC id={}, status={}", id, kyc.getStatus());
        return kycMapper.toDto(kyc);
    }

    public KycVerificationDto getByUserId(Long userId) {
        log.info("[KYC-READ] Fetching KYC for userId={}", userId);
        KycVerification kyc = kycRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("[KYC-READ] ❌ KYC not found for userId={}", userId);
                    return new ResourceNotFoundException("KYC Verification for user", userId);
                });
        log.info("[KYC-READ] ✅ Found KYC for userId={}, level={}", userId, kyc.getLevel());
        return kycMapper.toDto(kyc);
    }

    public List<KycVerificationDto> findByStatus(KycStatus status) {
        log.info("[KYC-READ] Listing KYC with status={}", status);
        List<KycVerificationDto> list = kycRepository.findByStatus(status).stream()
                .map(kycMapper::toDto)
                .toList();
        log.info("[KYC-READ] ✅ Found {} KYC records with status={}", list.size(), status);
        return list;
    }

    public List<KycVerificationDto> findAll() {
        log.info("[KYC-READ] Listing all KYC records");
        List<KycVerificationDto> list = kycRepository.findAllActive().stream()
                .map(kycMapper::toDto)
                .toList();
        log.info("[KYC-READ] ✅ Found {} KYC records total", list.size());
        return list;
    }
}
