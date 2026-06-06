package com.springbank.fraud.mapper;

import com.springbank.fraud.dto.FraudAlertDto;
import com.springbank.fraud.entity.FraudAlert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FraudAlertMapper {

    @Mapping(target = "triggeredRules",
            expression = "java(mapTriggeredRules(entity.getTriggeredRules()))")
    FraudAlertDto toDto(FraudAlert entity);

    @Mapping(target = "triggeredRules",
            expression = "java(mapRulesToString(dto.triggeredRules()))")
    FraudAlert toEntity(FraudAlertDto dto);

    default List<String> mapTriggeredRules(String rules) {
        if (rules == null || rules.isBlank()) return List.of();
        return Arrays.asList(rules.split(","));
    }

    default String mapRulesToString(List<String> rules) {
        if (rules == null) return "";
        return String.join(",", rules);
    }
}