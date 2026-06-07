package com.springbank.user.mapper;

import com.springbank.user.dto.KycVerificationDto;
import com.springbank.user.entity.KycVerification;
import com.springbank.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface KycMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = ".", target = "dailyTransferLimit", qualifiedByName = "dailyLimit")
    @Mapping(source = ".", target = "monthlyTransferLimit", qualifiedByName = "monthlyLimit")
    KycVerificationDto toDto(KycVerification entity);

    @Named("dailyLimit")
    default java.math.BigDecimal dailyLimit(KycVerification k) {
        return k.getDailyTransferLimit();
    }

    @Named("monthlyLimit")
    default java.math.BigDecimal monthlyLimit(KycVerification k) {
        return k.getMonthlyTransferLimit();
    }

    default User mapUser(Long id) {
        if (id == null) return null;
        User u = new User();
        u.setId(id);
        return u;
    }
}
