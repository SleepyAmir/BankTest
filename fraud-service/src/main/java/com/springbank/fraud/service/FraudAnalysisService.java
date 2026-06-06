package com.springbank.fraud.service;

import com.springbank.common.enums.AlertSeverity;
import com.springbank.common.enums.AlertStatus;
import com.springbank.common.enums.AmlAlertType;
import com.springbank.common.enums.FraudRiskLevel;
import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.fraud.dto.FraudAlertDto;
import com.springbank.fraud.dto.AmlAlertDto;
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

/**
 * ============================================================================
 * FRAUD ANALYSIS SERVICE
 * ============================================================================
 * Triggered by: RabbitMQ event from transaction-write (transaction.* routing)
 * Rules: Large TX (>50M), Unknown Device, (Time-based checks in future)
 * Output: FraudAlert (scored), AMLAlert (if risk >= 60)
 *
 * LOG MARKERS: [FRAUD-ANALYZE] [FRAUD-RULE] [FRAUD-SAVE] [FRAUD-AML] [FRAUD-QUERY]
 * ============================================================================
 */
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
        log.info("[FRAUD-ANALYZE] ==================== NEW FRAUD ANALYSIS ====================");
        log.info("[FRAUD-ANALYZE] TransactionId={}, TrackingCode={}, Amount={}, UserId={}",
                event.getTransactionId(), event.getTrackingCode(), event.getAmount(), event.getUserId());

        List<String> triggeredRules = new ArrayList<>();
        BigDecimal riskScore = BigDecimal.ZERO;

        // ====== RULE 1: Large transaction (>50,000,000) ======
        log.info("[FRAUD-RULE] Checking RULE-1: Large transaction (threshold=50,000,000)");
        if (event.getAmount().compareTo(new BigDecimal("50000000")) > 0) {
            triggeredRules.add("LARGE_TRANSACTION");
            riskScore = riskScore.add(new BigDecimal("30"));
            log.info("[FRAUD-RULE] ✅ RULE-1 TRIGGERED: amount={} > 50M. Risk +30. Total={}",
                    event.getAmount(), riskScore);
        } else {
            log.info("[FRAUD-RULE] ❌ RULE-1 not triggered (amount={} <= 50M)", event.getAmount());
        }

        // ====== RULE 2: Unusual time (placeholder for demo) ======
        log.info("[FRAUD-RULE] Checking RULE-2: Unusual time (simplified — always pass for demo)");

        // ====== RULE 3: Unknown device (no fingerprint) ======
        log.info("[FRAUD-RULE] Checking RULE-3: Device fingerprint");
        if (event.getDeviceFingerprint() == null || event.getDeviceFingerprint().isBlank()) {
            triggeredRules.add("UNKNOWN_DEVICE");
            riskScore = riskScore.add(new BigDecimal("20"));
            log.info("[FRAUD-RULE] ✅ RULE-3 TRIGGERED: Device fingerprint is empty. Risk +20. Total={}", riskScore);
        } else {
            log.info("[FRAUD-RULE] ❌ RULE-3 not triggered (deviceFingerprint={})", event.getDeviceFingerprint());
        }

        FraudRiskLevel level = determineRiskLevel(riskScore);
        log.info("[FRAUD-ANALYZE] Final Risk Score={}, Level={}", riskScore, level);

        // ====== Save FraudAlert ======
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
        log.info("[FRAUD-SAVE] ✅ FraudAlert saved: id={}, transactionId={}, riskLevel={}",
                alert.getId(), alert.getTransactionId(), alert.getRiskLevel());

        // ====== Check for AML alert (high risk) ======
        if (riskScore.compareTo(new BigDecimal("60")) >= 0) {
            log.info("[FRAUD-AML] Risk score {} >= 60 threshold → Creating AML alert", riskScore);
            createAmlAlert(event, triggeredRules, riskScore);
        } else {
            log.info("[FRAUD-AML] Risk score {} < 60 → No AML alert needed", riskScore);
        }

        log.info("[FRAUD-ANALYZE] ==================== ANALYSIS COMPLETE ====================\n");
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

        log.info("[FRAUD-AML] Creating AML alert: type={}, severity={}, score={}", type, severity, score);

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
        log.info("[FRAUD-AML] ✅ AML alert saved: id={}, type={}, severity={}, transactionId={}",
                aml.getId(), aml.getType(), aml.getSeverity(), aml.getTransactionId());
    }

    @Transactional(readOnly = true)
    public List<FraudAlertDto> getFraudAlertsByUser(Long userId) {
        log.info("[FRAUD-QUERY] Querying fraud alerts for userId={}", userId);
        List<FraudAlertDto> result = fraudAlertRepository.findByUserId(userId).stream()
                .map(fraudAlertMapper::toDto)
                .collect(Collectors.toList());
        log.info("[FRAUD-QUERY] Found {} fraud alerts for userId={}", result.size(), userId);
        return result;
    }

    @Transactional(readOnly = true)
    public List<FraudAlertDto> getAllFraudAlerts() {
        log.info("[FRAUD-QUERY] Querying all fraud alerts");
        List<FraudAlertDto> result = fraudAlertRepository.findAll().stream()
                .map(fraudAlertMapper::toDto)
                .collect(Collectors.toList());
        log.info("[FRAUD-QUERY] Found {} total fraud alerts", result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<AmlAlertDto> getAmlAlertsByUser(Long userId) {
        log.info("[FRAUD-QUERY] Querying AML alerts for userId={}", userId);
        List<AmlAlertDto> result = amlAlertRepository.findByUserId(userId).stream()
                .map(amlAlertMapper::toDto)
                .collect(Collectors.toList());
        log.info("[FRAUD-QUERY] Found {} AML alerts for userId={}", result.size(), userId);
        return result;
    }

    @Transactional
    public FraudAlertDto reviewFraudAlert(Long id, String reviewedBy, String note, boolean userConfirmed) {
        log.info("[FRAUD-REVIEW] Reviewing fraud alert id={} by {}. Confirmed={}", id, reviewedBy, userConfirmed);
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("[FRAUD-REVIEW] ❌ Fraud alert not found: id={}", id);
                    return new RuntimeException("Fraud alert not found: " + id);
                });
        alert.setReviewedBy(reviewedBy);
        alert.setReviewNote(note);
        alert.setUserConfirmed(userConfirmed);
        alert.setResolvedAt(LocalDateTime.now());
        FraudAlert saved = fraudAlertRepository.save(alert);
        log.info("[FRAUD-REVIEW] ✅ Fraud alert id={} reviewed. ResolvedAt={}", saved.getId(), saved.getResolvedAt());
        return fraudAlertMapper.toDto(saved);
    }
}
