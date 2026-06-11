package com.springbank.fraud.mapper;

import com.springbank.common.enums.FraudRiskLevel;
import com.springbank.fraud.dto.FraudAlertDto;
import com.springbank.fraud.entity.FraudAlert;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-11T21:09:13+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class FraudAlertMapperImpl implements FraudAlertMapper {

    @Override
    public FraudAlertDto toDto(FraudAlert entity) {
        if ( entity == null ) {
            return null;
        }

        Long id = null;
        BigDecimal riskScore = null;
        FraudRiskLevel riskLevel = null;
        String deviceFingerprint = null;
        String ipAddress = null;
        String location = null;
        String reviewedBy = null;
        String reviewNote = null;
        LocalDateTime resolvedAt = null;
        Boolean userConfirmed = null;
        Long transactionId = null;
        Long userId = null;
        LocalDateTime createdAt = null;

        id = entity.getId();
        riskScore = entity.getRiskScore();
        riskLevel = entity.getRiskLevel();
        deviceFingerprint = entity.getDeviceFingerprint();
        ipAddress = entity.getIpAddress();
        location = entity.getLocation();
        reviewedBy = entity.getReviewedBy();
        reviewNote = entity.getReviewNote();
        resolvedAt = entity.getResolvedAt();
        userConfirmed = entity.getUserConfirmed();
        transactionId = entity.getTransactionId();
        userId = entity.getUserId();
        createdAt = entity.getCreatedAt();

        List<String> triggeredRules = mapTriggeredRules(entity.getTriggeredRules());

        FraudAlertDto fraudAlertDto = new FraudAlertDto( id, riskScore, riskLevel, triggeredRules, deviceFingerprint, ipAddress, location, reviewedBy, reviewNote, resolvedAt, userConfirmed, transactionId, userId, createdAt );

        return fraudAlertDto;
    }

    @Override
    public FraudAlert toEntity(FraudAlertDto dto) {
        if ( dto == null ) {
            return null;
        }

        FraudAlert.FraudAlertBuilder<?, ?> fraudAlert = FraudAlert.builder();

        fraudAlert.createdAt( dto.createdAt() );
        fraudAlert.id( dto.id() );
        fraudAlert.riskScore( dto.riskScore() );
        fraudAlert.riskLevel( dto.riskLevel() );
        fraudAlert.deviceFingerprint( dto.deviceFingerprint() );
        fraudAlert.ipAddress( dto.ipAddress() );
        fraudAlert.location( dto.location() );
        fraudAlert.reviewedBy( dto.reviewedBy() );
        fraudAlert.reviewNote( dto.reviewNote() );
        fraudAlert.resolvedAt( dto.resolvedAt() );
        fraudAlert.userConfirmed( dto.userConfirmed() );
        fraudAlert.transactionId( dto.transactionId() );
        fraudAlert.userId( dto.userId() );

        fraudAlert.triggeredRules( mapRulesToString(dto.triggeredRules()) );

        return fraudAlert.build();
    }
}
