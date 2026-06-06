package com.springbank.fraud.mapper;

import com.springbank.fraud.dto.AmlAlertDto;
import com.springbank.fraud.entity.AmlAlert;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AmlAlertMapper {
    AmlAlertDto toDto(AmlAlert entity);
    AmlAlert toEntity(AmlAlertDto dto);
}
