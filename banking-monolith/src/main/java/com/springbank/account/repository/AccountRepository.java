package com.springbank.account.repository;

import com.springbank.account.entity.Account;
import com.springbank.common.repository.BaseEntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends BaseEntityRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByUserId(Long userId);

    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.deleted = false")
    List<Account> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT a.balance FROM Account a WHERE a.id = :id AND a.deleted = false")
    Optional<java.math.BigDecimal> findBalanceById(@Param("id") Long id);
}
