package com.springbank.card.mapper;

import com.springbank.account.entity.Account;
import com.springbank.card.dto.CardCreateDto;
import com.springbank.card.dto.CardResponseDto;
import com.springbank.card.dto.CardUpdateDto;
import com.springbank.card.entity.Card;
import com.springbank.common.enums.CardStatus;
import com.springbank.common.enums.CardType;
import com.springbank.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-11T22:56:01+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class CardMapperImpl implements CardMapper {

    @Autowired
    private CardSecretMapper cardSecretMapper;

    @Override
    public Card toEntity(CardCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        Card.CardBuilder<?, ?> card = Card.builder();

        card.cardNumber( dto.cardNumber() );
        card.cvv2( dto.cvv2() );
        card.pin( dto.pin() );
        card.type( dto.type() );
        card.expiryDate( dto.expiryDate() );
        card.isContactless( dto.isContactless() );
        card.dailyLimit( dto.dailyLimit() );
        card.monthlyLimit( dto.monthlyLimit() );

        card.account( mapAccount(dto.accountId()) );

        return card.build();
    }

    @Override
    public CardResponseDto toDto(Card card) {
        if ( card == null ) {
            return null;
        }

        Long accountId = null;
        Long userId = null;
        String cvv2 = null;
        Long id = null;
        String cardNumber = null;
        CardType type = null;
        CardStatus status = null;
        LocalDate expiryDate = null;
        Boolean isContactless = null;
        BigDecimal dailyLimit = null;
        BigDecimal monthlyLimit = null;
        BigDecimal monthlySpent = null;

        accountId = cardAccountId( card );
        userId = cardAccountUserId( card );
        cvv2 = cardSecretMapper.decryptCvv( card.getCvv2() );
        id = card.getId();
        cardNumber = card.getCardNumber();
        type = card.getType();
        status = card.getStatus();
        expiryDate = card.getExpiryDate();
        isContactless = card.getIsContactless();
        dailyLimit = card.getDailyLimit();
        monthlyLimit = card.getMonthlyLimit();
        monthlySpent = card.getMonthlySpent();

        CardResponseDto cardResponseDto = new CardResponseDto( id, cardNumber, cvv2, type, status, expiryDate, isContactless, dailyLimit, monthlyLimit, monthlySpent, accountId, userId );

        return cardResponseDto;
    }

    @Override
    public void updateFromDto(CardUpdateDto dto, Card card) {
        if ( dto == null ) {
            return;
        }

        if ( dto.status() != null ) {
            card.setStatus( dto.status() );
        }
        if ( dto.dailyLimit() != null ) {
            card.setDailyLimit( dto.dailyLimit() );
        }
        if ( dto.monthlyLimit() != null ) {
            card.setMonthlyLimit( dto.monthlyLimit() );
        }
    }

    private Long cardAccountId(Card card) {
        if ( card == null ) {
            return null;
        }
        Account account = card.getAccount();
        if ( account == null ) {
            return null;
        }
        Long id = account.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long cardAccountUserId(Card card) {
        if ( card == null ) {
            return null;
        }
        Account account = card.getAccount();
        if ( account == null ) {
            return null;
        }
        User user = account.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
