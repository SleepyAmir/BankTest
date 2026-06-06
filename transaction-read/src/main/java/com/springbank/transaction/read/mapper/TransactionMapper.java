package com.springbank.transaction.read.mapper;

import com.springbank.transaction.read.dto.response.TransactionResponseDto;
import com.springbank.transaction.read.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TransactionMapper {
    TransactionResponseDto toDto(Transaction entity);
}
