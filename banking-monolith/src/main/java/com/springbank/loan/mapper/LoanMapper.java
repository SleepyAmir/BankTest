package com.springbank.loan.mapper;

import com.springbank.loan.dto.LoanCreateDto;
import com.springbank.loan.dto.LoanResponseDto;
import com.springbank.loan.dto.LoanUpdateDto;
import com.springbank.loan.entity.Loan;
import com.springbank.account.entity.Account;
import com.springbank.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LoanMapper {

    @Mapping(target = "user", expression = "java(mapUser(dto.userId()))")
    @Mapping(target = "account", expression = "java(mapAccount(dto.accountId()))")
    Loan toEntity(LoanCreateDto dto);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "account.id", target = "accountId")
    LoanResponseDto toDto(Loan loan);

    void updateFromDto(LoanUpdateDto dto, @MappingTarget Loan loan);

    default User mapUser(Long id) {
        if (id == null) return null;
        User u = new User(); u.setId(id); return u;
    }
    default Account mapAccount(Long id) {
        if (id == null) return null;
        Account a = new Account(); a.setId(id); return a;
    }
}
