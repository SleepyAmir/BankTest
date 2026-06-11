package com.springbank.transaction.write.mapper;

import com.springbank.common.enums.TransactionStatus;
import com.springbank.common.enums.TransactionType;
import com.springbank.transaction.write.dto.TransactionResponseDto;
import com.springbank.transaction.write.dto.request.TransactionCreateDto;
import com.springbank.transaction.write.entity.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-11T21:09:11+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class TransactionMapperImpl implements TransactionMapper {

    @Override
    public Transaction toEntity(TransactionCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        Transaction.TransactionBuilder<?, ?> transaction = Transaction.builder();

        transaction.amount( dto.amount() );
        transaction.currency( dto.currency() );
        transaction.type( dto.type() );
        transaction.description( dto.description() );
        transaction.spendingCategory( dto.spendingCategory() );
        transaction.ipAddress( dto.ipAddress() );
        transaction.deviceFingerprint( dto.deviceFingerprint() );
        transaction.location( dto.location() );
        transaction.fee( dto.fee() );
        transaction.fromAccountId( dto.fromAccountId() );
        transaction.toAccountId( dto.toAccountId() );
        transaction.cardId( dto.cardId() );
        transaction.loanInstallmentId( dto.loanInstallmentId() );
        transaction.userId( dto.userId() );

        return transaction.build();
    }

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
