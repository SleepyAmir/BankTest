package com.springbank.analytics.mapper;

import com.springbank.analytics.dto.SpendingSnapshotDto;
import com.springbank.analytics.entity.SpendingSnapshot;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SpendingSnapshotMapper {
    SpendingSnapshotDto toDto(SpendingSnapshot entity);
}
