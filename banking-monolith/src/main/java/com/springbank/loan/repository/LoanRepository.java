package com.springbank.loan.repository;

import com.springbank.common.enums.LoanStatus;
import com.springbank.common.repository.BaseEntityRepository;
import com.springbank.loan.entity.Loan;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends BaseEntityRepository<Loan, Long> {

    List<Loan> findByUserId(Long userId);

    List<Loan> findByStatus(LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.user.id = :userId AND l.status IN (com.springbank.common.enums.LoanStatus.ACTIVE, com.springbank.common.enums.LoanStatus.PENDING)")
    List<Loan> findActiveOrPendingByUserId(@Param("userId") Long userId);

    Optional<Loan> findByIdAndUserId(Long id, Long userId);
}
