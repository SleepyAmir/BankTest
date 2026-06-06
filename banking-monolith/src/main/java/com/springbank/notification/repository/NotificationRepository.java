package com.springbank.notification.repository;

import com.springbank.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.user WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.user WHERE n.user.id = :userId AND n.isRead = false")
    List<Notification> findByUserIdAndIsReadFalse(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.user ORDER BY n.createdAt DESC")
    List<Notification> findAllWithUser();

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.user WHERE n.id = :id")
    java.util.Optional<Notification> findByIdWithUser(@Param("id") Long id);
}
