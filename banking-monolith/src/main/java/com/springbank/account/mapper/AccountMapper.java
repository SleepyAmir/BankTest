package com.springbank.account.mapper;

import com.springbank.account.dto.AccountCreateDto;
import com.springbank.account.dto.AccountResponseDto;
import com.springbank.account.dto.AccountUpdateDto;
import com.springbank.account.entity.Account;
import com.springbank.account.entity.Branch;
import com.springbank.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AccountMapper {

    @Mapping(target = "user", expression = "java(mapUser(dto.userId()))")
    @Mapping(target = "branch", expression = "java(mapBranch(dto.branchId()))")
    Account toEntity(AccountCreateDto dto);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "branch.id", target = "branchId")
    AccountResponseDto toDto(Account account);

    void updateFromDto(AccountUpdateDto dto, @MappingTarget Account account);

    default User mapUser(Long id) {
        if (id == null) return null;
        User u = new User();
        u.setId(id);
        return u;
    }

    default Branch mapBranch(Long id) {
        if (id == null) return null;
        Branch b = new Branch();
        b.setId(id);
        return b;
    }
}
