package com.springbank.notification.entity;

import com.springbank.common.entity.BaseEntity;
import com.springbank.common.enums.NotificationChannel;
import com.springbank.common.enums.NotificationType;
import com.springbank.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.IN_APP;

    // ======== RELATIONS ========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Transaction is in a different microservice — store ID only
    @Column(name = "transaction_id")
    private Long transactionId;

    // Loan is in the same monolith — store ID only (not JPA relation for flexibility)
    @Column(name = "loan_id")
    private Long loanId;
}