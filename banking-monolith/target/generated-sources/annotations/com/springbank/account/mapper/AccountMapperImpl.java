package com.springbank.account.mapper;

import com.springbank.account.dto.AccountCreateDto;
import com.springbank.account.dto.AccountResponseDto;
import com.springbank.account.dto.AccountUpdateDto;
import com.springbank.account.entity.Account;
import com.springbank.account.entity.Branch;
import com.springbank.common.enums.AccountStatus;
import com.springbank.common.enums.AccountType;
import com.springbank.user.entity.User;
import java.math.BigDecimal;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-10T12:01:24+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class AccountMapperImpl implements AccountMapper {

    @Override
    public Account toEntity(AccountCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        Account.AccountBuilder<?, ?> account = Account.builder();

        account.accountNumber( dto.accountNumber() );
        account.type( dto.type() );
        account.alias( dto.alias() );
        account.dailyTransferLimit( dto.dailyTransferLimit() );
        account.monthlyTransferLimit( dto.monthlyTransferLimit() );

        account.user( mapUser(dto.userId()) );
        account.branch( mapBranch(dto.branchId()) );

        return account.build();
    }

    @Override
    public AccountResponseDto toDto(Account account) {
        if ( account == null ) {
            return null;
        }

        Long userId = null;
        Long branchId = null;
        Long id = null;
        String accountNumber = null;
        AccountType type = null;
        BigDecimal balance = null;
        AccountStatus status = null;
        String alias = null;
        BigDecimal dailyTransferLimit = null;
        BigDecimal monthlyTransferLimit = null;

        userId = accountUserId( account );
        branchId = accountBranchId( account );
        id = account.getId();
        accountNumber = account.getAccountNumber();
        type = account.getType();
        balance = account.getBalance();
        status = account.getStatus();
        alias = account.getAlias();
        dailyTransferLimit = account.getDailyTransferLimit();
        monthlyTransferLimit = account.getMonthlyTransferLimit();

        AccountResponseDto accountResponseDto = new AccountResponseDto( id, accountNumber, type, balance, status, alias, dailyTransferLimit, monthlyTransferLimit, userId, branchId );

        return accountResponseDto;
    }

    @Override
    public void updateFromDto(AccountUpdateDto dto, Account account) {
        if ( dto == null ) {
            return;
        }

        if ( dto.status() != null ) {
            account.setStatus( dto.status() );
        }
        if ( dto.alias() != null ) {
            account.setAlias( dto.alias() );
        }
        if ( dto.dailyTransferLimit() != null ) {
            account.setDailyTransferLimit( dto.dailyTransferLimit() );
        }
        if ( dto.monthlyTransferLimit() != null ) {
            account.setMonthlyTransferLimit( dto.monthlyTransferLimit() );
        }
    }

    private Long accountUserId(Account account) {
        if ( account == null ) {
            return null;
        }
        User user = account.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long accountBranchId(Account account) {
        if ( account == null ) {
            return null;
        }
        Branch branch = account.getBranch();
        if ( branch == null ) {
            return null;
        }
        Long id = branch.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
