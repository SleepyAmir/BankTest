package com.springbank.fraud.repository;

import com.springbank.common.enums.AlertStatus;
import com.springbank.fraud.entity.AmlAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmlAlertRepository extends JpaRepository<AmlAlert, Long> {
    List<AmlAlert> findByUserId(Long userId);
    List<AmlAlert> findByTransactionId(Long transactionId);
    List<AmlAlert> findByStatus(AlertStatus status);
}
