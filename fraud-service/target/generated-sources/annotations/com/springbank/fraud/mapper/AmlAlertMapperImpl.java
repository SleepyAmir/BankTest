package com.springbank.fraud.mapper;

import com.springbank.common.enums.AlertSeverity;
import com.springbank.common.enums.AlertStatus;
import com.springbank.common.enums.AmlAlertType;
import com.springbank.fraud.dto.AmlAlertDto;
import com.springbank.fraud.entity.AmlAlert;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-06T19:40:37+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class AmlAlertMapperImpl implements AmlAlertMapper {

    @Override
    public AmlAlertDto toDto(AmlAlert entity) {
        if ( entity == null ) {
            return null;
        }

        Long id = null;
        AmlAlertType type = null;
        AlertSeverity severity = null;
        AlertStatus status = null;
        BigDecimal riskScore = null;
        String description = null;
        String investigatorNote = null;
        String investigatorUsername = null;
        LocalDateTime resolvedAt = null;
        Long userId = null;
        Long transactionId = null;
        LocalDateTime createdAt = null;

        id = entity.getId();
        type = entity.getType();
        severity = entity.getSeverity();
        status = entity.getStatus();
        riskScore = entity.getRiskScore();
        description = entity.getDescription();
        investigatorNote = entity.getInvestigatorNote();
        investigatorUsername = entity.getInvestigatorUsername();
        resolvedAt = entity.getResolvedAt();
        userId = entity.getUserId();
        transactionId = entity.getTransactionId();
        createdAt = entity.getCreatedAt();

        AmlAlertDto amlAlertDto = new AmlAlertDto( id, type, severity, status, riskScore, description, investigatorNote, investigatorUsername, resolvedAt, userId, transactionId, createdAt );

        return amlAlertDto;
    }

    @Override
    public AmlAlert toEntity(AmlAlertDto dto) {
        if ( dto == null ) {
            return null;
        }

        AmlAlert.AmlAlertBuilder<?, ?> amlAlert = AmlAlert.builder();

        amlAlert.createdAt( dto.createdAt() );
        amlAlert.id( dto.id() );
        amlAlert.type( dto.type() );
        amlAlert.severity( dto.severity() );
        amlAlert.status( dto.status() );
        amlAlert.riskScore( dto.riskScore() );
        amlAlert.description( dto.description() );
        amlAlert.investigatorNote( dto.investigatorNote() );
        amlAlert.investigatorUsername( dto.investigatorUsername() );
        amlAlert.resolvedAt( dto.resolvedAt() );
        amlAlert.userId( dto.userId() );
        amlAlert.transactionId( dto.transactionId() );

        return amlAlert.build();
    }
}
