package com.springbank.user.entity;
import com.springbank.user.entity.User;
import com.springbank.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_devices")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class UserDevice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // اثر انگشت منحصربه‌فرد دستگاه (ترکیب User-Agent، IP، و سایر مشخصات)
    @Column(name = "device_fingerprint", nullable = false, length = 200)
    private String deviceFingerprint;

    @Column(name = "device_type", length = 50)
    private String deviceType; // MOBILE, DESKTOP, TABLET

    @Column(length = 50)
    private String os; // Android, iOS, Windows, macOS

    @Column(length = 100)
    private String browser;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;

    @Column(name = "last_ip_address", length = 45)
    private String lastIpAddress;

    // آیا کاربر این دستگاه را تأیید کرده؟ (کاهش ریسک)
    @Column(name = "is_trusted")
    @Builder.Default
    private Boolean isTrusted = false;

    @Column(name = "trusted_at")
    private LocalDateTime trustedAt;

    // ======== RELATIONS ========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ======== HELPER METHODS ========

    // Fraud Detection: دستگاه جدید = ریسک بالاتر
    public boolean isNew() {
        return !isTrusted && firstSeenAt != null &&
               firstSeenAt.isAfter(LocalDateTime.now().minusDays(7));
    }

    public void markAsTrusted() {
        this.isTrusted = true;
        this.trustedAt = LocalDateTime.now();
    }
}
