package com.springbank.user.entity;
import com.springbank.user.entity.User;
import com.springbank.common.entity.BaseEntity;

import com.springbank.common.enums.KycLevel;
import com.springbank.common.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_verifications")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class KycVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KycStatus status = KycStatus.PENDING;
    // PENDING, DOCUMENT_UPLOADED, UNDER_REVIEW, APPROVED, REJECTED

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KycLevel level = KycLevel.BASIC;
    // BASIC, STANDARD, ENHANCED

    // مسیر تصویر کارت ملی (ذخیره در Object Storage)
    @Column(name = "national_id_image_path")
    private String nationalIdImagePath;

    // مسیر عکس سلفی
    @Column(name = "selfie_image_path")
    private String selfieImagePath;

    // مسیر مدرک آدرس
    @Column(name = "address_proof_path")
    private String addressProofPath;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // username کارمندی که تأیید کرد
    @Column(name = "verified_by", length = 50)
    private String verifiedBy;

    // ======== RELATIONS ========

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ======== HELPER METHODS ========

    public boolean isApproved() {
        return status == KycStatus.APPROVED;
    }

    // سقف انتقال روزانه بر اساس سطح KYC
    public BigDecimal getDailyTransferLimit() {
        return switch (level) {
            case BASIC    -> new BigDecimal("5000000");   // 5M تومان
            case STANDARD -> new BigDecimal("50000000");  // 50M تومان
            case ENHANCED -> new BigDecimal("500000000"); // 500M تومان
        };
    }

    // سقف انتقال ماهانه بر اساس سطح KYC
    public BigDecimal getMonthlyTransferLimit() {
        return switch (level) {
            case BASIC    -> new BigDecimal("20000000");
            case STANDARD -> new BigDecimal("200000000");
            case ENHANCED -> new BigDecimal("2000000000");
        };
    }
}
