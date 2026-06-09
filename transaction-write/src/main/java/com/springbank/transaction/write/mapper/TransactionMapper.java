package com.springbank.transaction.write.mapper;

import com.springbank.transaction.write.dto.response.TransactionResponseDto;
import com.springbank.transaction.write.dto.request.TransactionCreateDto;
import com.springbank.transaction.write.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TransactionMapper {
    Transaction toEntity(TransactionCreateDto dto);
    TransactionResponseDto toDto(Transaction entity);
}
