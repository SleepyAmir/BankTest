package com.springbank.fraud.service;

import com.springbank.common.enums.FraudRiskLevel;
import com.springbank.common.event.FraudDetectedEvent;
import com.springbank.common.event.TransactionBlockedEvent;
import com.springbank.common.event.TransactionCompletedEvent;
import com.springbank.fraud.dto.AmlAlertDto;
import com.springbank.fraud.dto.FraudAlertDto;
import com.springbank.fraud.entity.FraudAlert;
import com.springbank.fraud.entity.TransactionHistory;
import com.springbank.fraud.mapper.AmlAlertMapper;
import com.springbank.fraud.mapper.FraudAlertMapper;
import com.springbank.fraud.messaging.FraudEventPublisher;
import com.springbank.fraud.repository.AmlAlertRepository;
import com.springbank.fraud.repository.FraudAlertRepository;
import com.springbank.fraud.repository.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * FRAUD ANALYSIS SERVICE — تشخیص تقلب (فلوی ۶)
 * ============================================================================
 * همزمان با هر TransactionCompletedEvent یک FraudAlert ساخته می‌شود و قوانین زیر
 * اجرا می‌شوند (هر قانون به riskScore امتیاز اضافه می‌کند):
 *
 *   - VELOCITY_CHECK : بیش از ۳ تراکنش در یک دقیقه‌ی اخیر  → +۴۰
 *   - UNUSUAL_TIME   : تراکنش در ساعات مشکوک (۰۰:۰۰ تا ۰۵:۰۰) → +۲۰
 *   - AMOUNT_ANOMALY : مبلغ به‌مراتب بالاتر از میانگین ماهانه (×۵) → +۳۵
 *   - UNKNOWN_DEVICE : نبود اثرانگشت دستگاه → +۱۵
 *
 * سطح ریسک بر اساس امتیاز نهایی تعیین می‌شود:
 *   >=۸۰ BLOCK | >=۶۰ CHALLENGE | >=۳۰ REVIEW | بقیه ALLOW
 *
 * در صورت BLOCK، یک TransactionBlockedEvent منتشر می‌شود تا وضعیت تراکنش BLOCKED شود.
 * سپس بررسی‌های AML (فلوی ۷) اجرا می‌شوند.
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAnalysisService {

    private final FraudAlertRepository fraudAlertRepository;
    private final AmlAlertRepository amlAlertRepository;
    private final TransactionHistoryRepository historyRepository;
    private final FraudAlertMapper fraudAlertMapper;
    private final AmlAlertMapper amlAlertMapper;
    private final AmlAnalysisService amlAnalysisService;
    private final FraudEventPublisher eventPublisher;

    // آستانه‌ها و امتیازها
    private static final int VELOCITY_MAX_PER_MINUTE = 3;
    private static final BigDecimal SCORE_VELOCITY = new BigDecimal("40");
    private static final BigDecimal SCORE_UNUSUAL_TIME = new BigDecimal("20");
    private static final BigDecimal SCORE_AMOUNT_ANOMALY = new BigDecimal("35");
    private static final BigDecimal SCORE_UNKNOWN_DEVICE = new BigDecimal("15");
    private static final BigDecimal ANOMALY_MULTIPLIER = new BigDecimal("5"); // ۵ برابر میانگین

    @Transactional
    public void analyzeTransaction(TransactionCompletedEvent event) {
        log.info("[FRAUD-ANALYZE] شروع تحلیل: trackingCode={}, amount={}, userId={}",
                event.getTrackingCode(), event.getAmount(), event.getUserId());

        LocalDateTime now = event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now();
        List<String> triggered = new ArrayList<>();
        BigDecimal riskScore = BigDecimal.ZERO;

        // ===== RULE 1: VELOCITY_CHECK (بیش از ۳ تراکنش در دقیقه) =====
        long lastMinuteCount = historyRepository.countByUserIdAndOccurredAtAfter(
                event.getUserId(), now.minusMinutes(1));
        if (lastMinuteCount > VELOCITY_MAX_PER_MINUTE) {
            triggered.add("VELOCITY_CHECK");
            riskScore = riskScore.add(SCORE_VELOCITY);
            log.warn("[FRAUD-RULE] ✅ VELOCITY_CHECK: {} تراکنش در دقیقه‌ی اخیر (> {})",
                    lastMinuteCount, VELOCITY_MAX_PER_MINUTE);
        }

        // ===== RULE 2: UNUSUAL_TIME (نیمه‌شب ۰۰:۰۰–۰۵:۰۰) =====
        int hour = now.getHour();
        if (hour >= 0 && hour < 5) {
            triggered.add("UNUSUAL_TIME");
            riskScore = riskScore.add(SCORE_UNUSUAL_TIME);
            log.warn("[FRAUD-RULE] ✅ UNUSUAL_TIME: تراکنش در ساعت مشکوک {}", hour);
        }

        // ===== RULE 3: AMOUNT_ANOMALY (بالاتر از میانگین ماهانه) =====
        BigDecimal monthlyAvg = historyRepository.averageAmountSince(
                event.getUserId(), now.minusDays(30));
        if (monthlyAvg != null && monthlyAvg.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal threshold = monthlyAvg.multiply(ANOMALY_MULTIPLIER);
            if (event.getAmount().compareTo(threshold) > 0) {
                triggered.add("AMOUNT_ANOMALY");
                riskScore = riskScore.add(SCORE_AMOUNT_ANOMALY);
                log.warn("[FRAUD-RULE] ✅ AMOUNT_ANOMALY: مبلغ {} بسیار بالاتر از میانگین ماهانه {}",
                        event.getAmount(), monthlyAvg.setScale(0, RoundingMode.HALF_UP));
            }
        }

        // ===== RULE 4: UNKNOWN_DEVICE =====
        if (event.getDeviceFingerprint() == null || event.getDeviceFingerprint().isBlank()) {
            triggered.add("UNKNOWN_DEVICE");
            riskScore = riskScore.add(SCORE_UNKNOWN_DEVICE);
            log.info("[FRAUD-RULE] ✅ UNKNOWN_DEVICE: اثرانگشت دستگاه موجود نیست");
        }

        FraudRiskLevel level = determineRiskLevel(riskScore);
        log.info("[FRAUD-ANALYZE] امتیاز نهایی={}, سطح={}, قوانین={}", riskScore, level, triggered);

        // ===== ذخیره‌ی FraudAlert =====
        FraudAlert alert = FraudAlert.builder()
                .riskScore(riskScore)
                .riskLevel(level)
                .triggeredRules(String.join(",", triggered))
                .deviceFingerprint(event.getDeviceFingerprint())
                .ipAddress(event.getIpAddress())
                .location(event.getLocation())
                .transactionId(event.getTransactionId())
                .trackingCode(event.getTrackingCode())
                .userId(event.getUserId())
                .build();
        fraudAlertRepository.save(alert);
        log.info("[FRAUD-SAVE] ✅ FraudAlert ذخیره شد: id={}, level={}", alert.getId(), level);

        // ===== ثبت در تاریخچه (برای تحلیل‌های بعدی) =====
        saveHistory(event, now);

        // ===== اگر BLOCK → انتشار رویداد بلاک =====
        if (level == FraudRiskLevel.BLOCK) {
            eventPublisher.publishBlocked(TransactionBlockedEvent.builder()
                    .transactionId(event.getTransactionId())
                    .trackingCode(event.getTrackingCode())
                    .userId(event.getUserId())
                    .amount(event.getAmount())
                    .riskScore(riskScore)
                    .riskLevel(level.name())
                    .triggeredRules(triggered)
                    .reason("تراکنش به دلیل ریسک بالای تقلب مسدود شد")
                    .blockedAt(LocalDateTime.now())
                    .build());
        }

        // ===== اطلاع‌رسانی ریسک بالا (REVIEW به بالا) =====
        if (level != FraudRiskLevel.ALLOW) {
            eventPublisher.publishFraudDetected(FraudDetectedEvent.builder()
                    .fraudAlertId(alert.getId())
                    .transactionId(event.getTransactionId())
                    .userId(event.getUserId())
                    .riskScore(riskScore)
                    .riskLevel(level.name())
                    .triggeredRules(triggered)
                    .detectedAt(LocalDateTime.now())
                    .build());
        }

        // ===== فلوی ۷: بررسی‌های AML =====
        amlAnalysisService.analyze(event);

        log.info("[FRAUD-ANALYZE] ✅ تحلیل کامل شد برای trackingCode={}", event.getTrackingCode());
    }

    private void saveHistory(TransactionCompletedEvent event, LocalDateTime occurredAt) {
        TransactionHistory history = TransactionHistory.builder()
                .transactionId(event.getTransactionId())
                .trackingCode(event.getTrackingCode())
                .userId(event.getUserId())
                .fromAccountId(event.getFromAccountId())
                .toAccountId(event.getToAccountId())
                .amount(event.getAmount())
                .type(event.getType())
                .occurredAt(occurredAt)
                .build();
        historyRepository.save(history);
    }

    private FraudRiskLevel determineRiskLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("80")) >= 0) return FraudRiskLevel.BLOCK;
        if (score.compareTo(new BigDecimal("60")) >= 0) return FraudRiskLevel.CHALLENGE;
        if (score.compareTo(new BigDecimal("30")) >= 0) return FraudRiskLevel.REVIEW;
        return FraudRiskLevel.ALLOW;
    }

    // ===================== Query methods (پنل کارمند) =====================

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
    public List<FraudAlertDto> getBlockedAlerts() {
        return fraudAlertRepository.findByRiskLevel(FraudRiskLevel.BLOCK).stream()
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
        log.info("[FRAUD-REVIEW] بررسی FraudAlert id={} توسط {}", id, reviewedBy);
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fraud alert not found: " + id));
        alert.setReviewedBy(reviewedBy);
        alert.setReviewNote(note);
        alert.setUserConfirmed(userConfirmed);
        alert.setResolvedAt(LocalDateTime.now());
        return fraudAlertMapper.toDto(fraudAlertRepository.save(alert));
    }
}
