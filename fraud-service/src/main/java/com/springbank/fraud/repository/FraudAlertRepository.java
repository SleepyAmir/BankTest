package com.springbank.fraud.repository;

import com.springbank.fraud.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findByUserId(Long userId);
    List<FraudAlert> findByTransactionId(Long transactionId);
    Optional<FraudAlert> findByTransactionIdAndUserId(Long transactionId, Long userId);
    List<FraudAlert> findByRiskLevel(com.springbank.common.enums.FraudRiskLevel riskLevel);
}
