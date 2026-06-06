package com.springbank.loan.repository;

import com.springbank.common.repository.BaseEntityRepository;
import com.springbank.loan.entity.CreditScore;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditScoreRepository extends BaseEntityRepository<CreditScore, Long> {

    @Query("SELECT c FROM CreditScore c WHERE c.user.id = :userId AND c.deleted = false")
    Optional<CreditScore> findByUserId(@Param("userId") Long userId);
}
