package com.springbank.transaction.read.mapper;

import com.springbank.common.enums.TransactionStatus;
import com.springbank.common.enums.TransactionType;
import com.springbank.transaction.read.dto.response.TransactionResponseDto;
import com.springbank.transaction.read.entity.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-10T01:41:38+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class TransactionMapperImpl implements TransactionMapper {

    @Override
    public TransactionResponseDto toDto(Transaction entity) {
        if ( entity == null ) {
            return null;
        }

        Long id = null;
        String trackingCode = null;
        BigDecimal amount = null;
        String currency = null;
        TransactionType type = null;
        TransactionStatus status = null;
        String description = null;
        String referenceNo = null;
        String spendingCategory = null;
        Long fromAccountId = null;
        Long toAccountId = null;
        Long cardId = null;
        Long loanInstallmentId = null;
        String ipAddress = null;
        String deviceFingerprint = null;
        String location = null;
        LocalDateTime createdAt = null;

        id = entity.getId();
        trackingCode = entity.getTrackingCode();
        amount = entity.getAmount();
        currency = entity.getCurrency();
        type = entity.getType();
        status = entity.getStatus();
        description = entity.getDescription();
        referenceNo = entity.getReferenceNo();
        spendingCategory = entity.getSpendingCategory();
        fromAccountId = entity.getFromAccountId();
        toAccountId = entity.getToAccountId();
        cardId = entity.getCardId();
        loanInstallmentId = entity.getLoanInstallmentId();
        ipAddress = entity.getIpAddress();
        deviceFingerprint = entity.getDeviceFingerprint();
        location = entity.getLocation();
        createdAt = entity.getCreatedAt();

        TransactionResponseDto transactionResponseDto = new TransactionResponseDto( id, trackingCode, amount, currency, type, status, description, referenceNo, spendingCategory, fromAccountId, toAccountId, cardId, loanInstallmentId, ipAddress, deviceFingerprint, location, createdAt );

        return transactionResponseDto;
    }
}
