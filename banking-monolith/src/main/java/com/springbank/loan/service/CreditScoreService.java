package com.springbank.loan.service;

import com.springbank.loan.entity.CreditScore;
import com.springbank.loan.repository.CreditScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditScoreService {

    private final CreditScoreRepository creditScoreRepository;

    public BigDecimal getRecommendedInterestRate(Long userId) {
        return creditScoreRepository.findByUserId(userId)
                .map(CreditScore::getRecommendedInterestRate)
                .orElse(new BigDecimal("18.0")); // default rate
    }

    public CreditScore getCreditScore(Long userId) {
        return creditScoreRepository.findByUserId(userId)
                .orElse(null);
    }
}
