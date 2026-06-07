package com.springbank.user.service;

import com.springbank.common.enums.KycLevel;
import com.springbank.common.enums.KycStatus;
import com.springbank.common.exception.ResourceNotFoundException;
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

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KycWriteService {

    private final KycVerificationRepository kycRepository;
    private final UserRepository userRepository;
    private final KycMapper kycMapper;

    public KycVerificationDto submitKyc(KycSubmitDto dto) {
        log.info("[KYC-SUBMIT] Submitting KYC for userId={}, requestedLevel={}", dto.userId(), dto.requestedLevel());

        User user = userRepository.findActiveById(dto.userId())
                .orElseThrow(() -> {
                    log.error("[KYC-SUBMIT] ❌ User not found id={}", dto.userId());
                    return new ResourceNotFoundException("User", dto.userId());
                });

        KycVerification kyc = kycRepository.findByUserId(user.getId()).orElse(null);
        if (kyc == null) {
            kyc = KycVerification.builder()
                    .user(user)
                    .status(KycStatus.PENDING)
                    .level(dto.requestedLevel() != null ? dto.requestedLevel() : KycLevel.BASIC)
                    .nationalIdImagePath(dto.nationalIdImagePath())
                    .selfieImagePath(dto.selfieImagePath())
                    .addressProofPath(dto.addressProofPath())
                    .build();
            log.info("[KYC-SUBMIT] Creating new KYC record for userId={}", user.getId());
        } else {
            kyc.setStatus(KycStatus.PENDING);
            kyc.setNationalIdImagePath(dto.nationalIdImagePath());
            kyc.setSelfieImagePath(dto.selfieImagePath());
            kyc.setAddressProofPath(dto.addressProofPath());
            if (dto.requestedLevel() != null) kyc.setLevel(dto.requestedLevel());
            log.info("[KYC-SUBMIT] Updating existing KYC record id={} for userId={}", kyc.getId(), user.getId());
        }

        KycVerification saved = kycRepository.save(kyc);
        log.info("[KYC-SUBMIT] ✅ KYC submitted: id={}, status={}, level={}", saved.getId(), saved.getStatus(), saved.getLevel());
        return kycMapper.toDto(saved);
    }

    public KycVerificationDto reviewKyc(Long id, KycReviewDto dto, String reviewerUsername) {
        log.info("[KYC-REVIEW] Reviewing KYC id={} by reviewer={}", id, reviewerUsername);

        KycVerification kyc = kycRepository.findActiveById(id)
                .orElseThrow(() -> {
                    log.error("[KYC-REVIEW] ❌ KYC not found id={}", id);
                    return new ResourceNotFoundException("KYC Verification", id);
                });

        kyc.setStatus(dto.status());
        if (dto.approvedLevel() != null) {
            kyc.setLevel(dto.approvedLevel());
        }
        kyc.setRejectionReason(dto.rejectionReason());
        kyc.setVerifiedBy(reviewerUsername);
        kyc.setVerifiedAt(LocalDateTime.now());

        KycVerification saved = kycRepository.save(kyc);
        log.info("[KYC-REVIEW] ✅ KYC id={} reviewed. status={}, level={}, by={}",
                saved.getId(), saved.getStatus(), saved.getLevel(), reviewerUsername);
        return kycMapper.toDto(saved);
    }
}
