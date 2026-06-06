package com.springbank.fraud.service;

import com.springbank.common.enums.AlertSeverity;
import com.springbank.common.enums.AlertStatus;
import com.springbank.common.enums.AmlAlertType;
import com.springbank.common.enums.FraudRiskLevel;
import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.fraud.dto.FraudAlertDto;
import com.springbank.fraud.entity.AmlAlert;
import com.springbank.fraud.entity.FraudAlert;
import com.springbank.fraud.mapper.FraudAlertMapper;
import com.springbank.fraud.mapper.AmlAlertMapper;
import com.springbank.fraud.repository.FraudAlertRepository;
import com.springbank.fraud.repository.AmlAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAnalysisService {

    private final FraudAlertRepository fraudAlertRepository;
    private final AmlAlertRepository amlAlertRepository;
    private final FraudAlertMapper fraudAlertMapper;
    private final AmlAlertMapper amlAlertMapper;

    @Transactional
    public void analyzeTransaction(TransactionCompletedEvent event) {
        log.info("Analyzing transaction {} for fraud", event.getTransactionId());

        List<String> triggeredRules = new ArrayList<>();
        BigDecimal riskScore = BigDecimal.ZERO;

        // Rule 1: Large transaction (>50M)
        if (event.getAmount().compareTo(new BigDecimal("50000000")) > 0) {
            triggeredRules.add("LARGE_TRANSACTION");
            riskScore = riskScore.add(new BigDecimal("30"));
        }

        // Rule 2: Unusual time (simplified - always pass for demo)
        // In real implementation: check if transaction hour is unusual for user

        // Rule 3: New device (if no device fingerprint match)
        if (event.getDeviceFingerprint() == null || event.getDeviceFingerprint().isBlank()) {
            triggeredRules.add("UNKNOWN_DEVICE");
            riskScore = riskScore.add(new BigDecimal("20"));
        }

        FraudRiskLevel level = determineRiskLevel(riskScore);

        FraudAlert alert = FraudAlert.builder()
                .riskScore(riskScore)
                .riskLevel(level)
                .triggeredRules(String.join(",", triggeredRules))
                .deviceFingerprint(event.getDeviceFingerprint())
                .ipAddress(event.getIpAddress())
                .location(event.getLocation())
                .transactionId(event.getTransactionId())
                .userId(event.getUserId())
                .build();

        fraudAlertRepository.save(alert);

        // Check for AML alerts
        if (riskScore.compareTo(new BigDecimal("60")) >= 0) {
            createAmlAlert(event, triggeredRules, riskScore);
        }
    }

    private FraudRiskLevel determineRiskLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("80")) >= 0) return FraudRiskLevel.BLOCK;
        if (score.compareTo(new BigDecimal("60")) >= 0) return FraudRiskLevel.CHALLENGE;
        if (score.compareTo(new BigDecimal("30")) >= 0) return FraudRiskLevel.REVIEW;
        return FraudRiskLevel.ALLOW;
    }

    private void createAmlAlert(TransactionCompletedEvent event, List<String> rules, BigDecimal score) {
        AmlAlertType type = rules.contains("LARGE_TRANSACTION") ? AmlAlertType.LARGE_TRANSACTION : AmlAlertType.UNUSUAL_PATTERN;
        AlertSeverity severity = score.compareTo(new BigDecimal("80")) >= 0 ? AlertSeverity.CRITICAL : AlertSeverity.HIGH;

        AmlAlert aml = AmlAlert.builder()
                .type(type)
                .severity(severity)
                .status(AlertStatus.OPEN)
                .riskScore(score)
                .description("Auto-generated AML alert: " + String.join(", ", rules))
                .userId(event.getUserId())
                .transactionId(event.getTransactionId())
                .build();

        amlAlertRepository.save(aml);
    }

    @Transactional(readOnly = true)
    public List<FraudAlertDto> getFraudAlertsByUser(Long userId) {
        return fraudAlertRepository.findByUserId(userId).stream()
                .map(fraudAlertMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FraudAlertDto> getAllFraudAlerts() {
        return fraudAlertRepository.findAll().stream()
                .map(fraudAlertMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AmlAlertDto> getAmlAlertsByUser(Long userId) {
        return amlAlertRepository.findByUserId(userId).stream()
                .map(amlAlertMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public FraudAlertDto reviewFraudAlert(Long id, String reviewedBy, String note, boolean userConfirmed) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fraud alert not found: " + id));
        alert.setReviewedBy(reviewedBy);
        alert.setReviewNote(note);
        alert.setUserConfirmed(userConfirmed);
        alert.setResolvedAt(LocalDateTime.now());
        return fraudAlertMapper.toDto(fraudAlertRepository.save(alert));
    }
}
