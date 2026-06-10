package com.springbank.card.mapper;

import com.springbank.card.dto.CardCreateDto;
import com.springbank.card.dto.CardResponseDto;
import com.springbank.card.dto.CardUpdateDto;
import com.springbank.card.entity.Card;
import com.springbank.account.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = { CardSecretMapper.class })
public interface CardMapper {

    @Mapping(target = "account", expression = "java(mapAccount(dto.accountId()))")
    Card toEntity(CardCreateDto dto);

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "account.user.id", target = "userId")
    @Mapping(source = "cvv2", target = "cvv2", qualifiedByName = "decryptCvv")
    CardResponseDto toDto(Card card);

    void updateFromDto(CardUpdateDto dto, @MappingTarget Card card);

    default Account mapAccount(Long id) {
        if (id == null) return null;
        Account a = new Account();
        a.setId(id);
        return a;
    }
}
