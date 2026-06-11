package com.springbank.user.mapper;

import com.springbank.common.enums.KycLevel;
import com.springbank.common.enums.KycStatus;
import com.springbank.user.dto.KycVerificationDto;
import com.springbank.user.entity.KycVerification;
import com.springbank.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-11T20:36:07+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class KycMapperImpl implements KycMapper {

    @Override
    public KycVerificationDto toDto(KycVerification entity) {
        if ( entity == null ) {
            return null;
        }

        Long userId = null;
        String username = null;
        BigDecimal dailyTransferLimit = null;
        BigDecimal monthlyTransferLimit = null;
        Long id = null;
        KycStatus status = null;
        KycLevel level = null;
        String nationalIdImagePath = null;
        String selfieImagePath = null;
        String addressProofPath = null;
        String rejectionReason = null;
        String verifiedBy = null;
        LocalDateTime verifiedAt = null;

        userId = entityUserId( entity );
        username = entityUserUsername( entity );
        dailyTransferLimit = dailyLimit( entity );
        monthlyTransferLimit = monthlyLimit( entity );
        id = entity.getId();
        status = entity.getStatus();
        level = entity.getLevel();
        nationalIdImagePath = entity.getNationalIdImagePath();
        selfieImagePath = entity.getSelfieImagePath();
        addressProofPath = entity.getAddressProofPath();
        rejectionReason = entity.getRejectionReason();
        verifiedBy = entity.getVerifiedBy();
        verifiedAt = entity.getVerifiedAt();

        KycVerificationDto kycVerificationDto = new KycVerificationDto( id, userId, username, status, level, nationalIdImagePath, selfieImagePath, addressProofPath, rejectionReason, verifiedBy, verifiedAt, dailyTransferLimit, monthlyTransferLimit );

        return kycVerificationDto;
    }

    private Long entityUserId(KycVerification kycVerification) {
        if ( kycVerification == null ) {
            return null;
        }
        User user = kycVerification.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String entityUserUsername(KycVerification kycVerification) {
        if ( kycVerification == null ) {
            return null;
        }
        User user = kycVerification.getUser();
        if ( user == null ) {
            return null;
        }
        String username = user.getUsername();
        if ( username == null ) {
            return null;
        }
        return username;
    }
}
