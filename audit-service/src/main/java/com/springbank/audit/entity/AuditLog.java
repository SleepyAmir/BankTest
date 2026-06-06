package com.springbank.audit.entity;
import com.springbank.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class AuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // کاربری که عمل را انجام داد
    @Column(name = "actor_username", nullable = false, length = 50)
    private String actorUsername;

    // عمل انجام شده (مثلاً: APPROVE_LOAN, BLOCK_USER, CHANGE_LIMIT)
    @Column(nullable = false, length = 100)
    private String action;

    // نوع Entity تغییر یافته (مثلاً: User, account, Loan)
    @Column(name = "entity_type", length = 50)
    private String entityType;

    // ID رکورد تغییر یافته
    @Column(name = "entity_id")
    private Long entityId;

    // مقدار قبلی به صورت JSON
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    // مقدار جدید به صورت JSON
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 200)
    private String reason;

    // ======== HELPER METHODS ========

    // ساخت سریع لاگ
    public static AuditLog of(String actor, String action,
                               String entityType, Long entityId) {
        return AuditLog.builder()
                .actorUsername(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
