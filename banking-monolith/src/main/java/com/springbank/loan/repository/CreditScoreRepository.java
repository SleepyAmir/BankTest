package com.springbank.loan.repository;

import com.springbank.common.repository.BaseEntityRepository;
import com.springbank.loan.entity.CreditScore;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditScoreRepository extends BaseEntityRepository<CreditScore, Long> {
    Optional<CreditScore> findByUserId(Long userId);
}
