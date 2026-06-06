package com.springbank.user.entity;
import com.springbank.user.entity.User;
import com.springbank.common.entity.BaseEntity;


import com.springbank.common.enums.OtpChannel;
import com.springbank.common.enums.OtpPurpose;
import com.springbank.common.enums.OtpStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class OtpVerification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // کد OTP به صورت هش شده
    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;
    // LOGIN, HIGH_RISK_TRANSFER, PASSWORD_RESET, KYC_CONFIRM

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpChannel channel; // SMS, EMAIL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OtpStatus status = OtpStatus.PENDING;
    // PENDING, USED, EXPIRED, FAILED

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    // تعداد تلاش‌های اشتباه
    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    // حداکثر ۳ تلاش مجاز
    private static final int MAX_ATTEMPTS = 3;

    // ======== RELATIONS ========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ======== HELPER METHODS ========

    public boolean isValid() {
        return status == OtpStatus.PENDING
                && LocalDateTime.now().isBefore(expiresAt)
                && attempts < MAX_ATTEMPTS;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void incrementAttempts() {
        this.attempts++;
        if (this.attempts >= MAX_ATTEMPTS) {
            this.status = OtpStatus.FAILED;
        }
    }

    public void markAsUsed() {
        this.status = OtpStatus.USED;
        this.usedAt = LocalDateTime.now();
    }
}
