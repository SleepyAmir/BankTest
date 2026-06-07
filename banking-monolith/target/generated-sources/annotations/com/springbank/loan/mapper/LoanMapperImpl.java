package com.springbank.loan.mapper;

import com.springbank.account.entity.Account;
import com.springbank.common.enums.LoanStatus;
import com.springbank.loan.dto.LoanCreateDto;
import com.springbank.loan.dto.LoanResponseDto;
import com.springbank.loan.dto.LoanUpdateDto;
import com.springbank.loan.entity.Loan;
import com.springbank.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-07T11:01:52+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class LoanMapperImpl implements LoanMapper {

    @Override
    public Loan toEntity(LoanCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        Loan.LoanBuilder<?, ?> loan = Loan.builder();

        loan.amount( dto.amount() );
        loan.durationMonths( dto.durationMonths() );
        loan.purpose( dto.purpose() );

        loan.user( mapUser(dto.userId()) );
        loan.account( mapAccount(dto.accountId()) );

        return loan.build();
    }

    @Override
    public LoanResponseDto toDto(Loan loan) {
        if ( loan == null ) {
            return null;
        }

        Long userId = null;
        Long accountId = null;
        Long id = null;
        BigDecimal amount = null;
        BigDecimal interestRate = null;
        Integer durationMonths = null;
        BigDecimal monthlyInstallment = null;
        LoanStatus status = null;
        String purpose = null;
        LocalDateTime approvedAt = null;
        String approvedBy = null;
        LocalDate startDate = null;
        LocalDate endDate = null;
        BigDecimal remainingAmount = null;

        userId = loanUserId( loan );
        accountId = loanAccountId( loan );
        id = loan.getId();
        amount = loan.getAmount();
        interestRate = loan.getInterestRate();
        durationMonths = loan.getDurationMonths();
        monthlyInstallment = loan.getMonthlyInstallment();
        status = loan.getStatus();
        purpose = loan.getPurpose();
        approvedAt = loan.getApprovedAt();
        approvedBy = loan.getApprovedBy();
        startDate = loan.getStartDate();
        endDate = loan.getEndDate();
        remainingAmount = loan.getRemainingAmount();

        LoanResponseDto loanResponseDto = new LoanResponseDto( id, amount, interestRate, durationMonths, monthlyInstallment, status, purpose, approvedAt, approvedBy, startDate, endDate, remainingAmount, userId, accountId );

        return loanResponseDto;
    }

    @Override
    public void updateFromDto(LoanUpdateDto dto, Loan loan) {
        if ( dto == null ) {
            return;
        }

        if ( dto.interestRate() != null ) {
            loan.setInterestRate( dto.interestRate() );
        }
        if ( dto.purpose() != null ) {
            loan.setPurpose( dto.purpose() );
        }
    }

    private Long loanUserId(Loan loan) {
        if ( loan == null ) {
            return null;
        }
        User user = loan.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long loanAccountId(Loan loan) {
        if ( loan == null ) {
            return null;
        }
        Account account = loan.getAccount();
        if ( account == null ) {
            return null;
        }
        Long id = account.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
