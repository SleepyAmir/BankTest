package com.springbank.fraud.mapper;

import com.springbank.fraud.dto.FraudAlertDto;
import com.springbank.fraud.entity.FraudAlert;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FraudAlertMapper {
    FraudAlertDto toDto(FraudAlert entity);
    FraudAlert toEntity(FraudAlertDto dto);
}
