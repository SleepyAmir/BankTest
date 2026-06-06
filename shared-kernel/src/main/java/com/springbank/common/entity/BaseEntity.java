package com.springbank.common.entity;

import jakarta.persistence.*;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)

@NoArgsConstructor
@Getter
@Setter
@SuperBuilder

public class BaseEntity {

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    private Long deletedBy;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    @PreUpdate
    @PreRemove
    protected void onUpdate() {
        if (deleted) {
            throw new UnsupportedOperationException("Hard delete is not allowed. Use soft delete instead.");
        }
    }
}
