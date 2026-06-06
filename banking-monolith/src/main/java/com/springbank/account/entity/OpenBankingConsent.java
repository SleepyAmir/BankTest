package com.springbank.account.entity;
import com.springbank.user.entity.User;
import com.springbank.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "open_banking_consents")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class OpenBankingConsent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // شناسه اپلیکیشن خارجی که درخواست دسترسی کرده
    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "client_name", length = 100)
    private String clientName;

    // توکن یکتا برای احراز هویت درخواست‌ها
    @Column(name = "consent_token", nullable = false, unique = true, length = 200)
    private String consentToken;

    // دسترسی‌های مجاز (JSON Array)
    // مثال: ["READ_BALANCE","READ_TRANSACTIONS"]
    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    // ======== RELATIONS ========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // ======== HELPER METHODS ========

    public boolean isValid() {
        return active && LocalDateTime.now().isBefore(expiresAt);
    }

    public void revoke() {
        this.active = false;
        this.revokedAt = LocalDateTime.now();
    }
}
