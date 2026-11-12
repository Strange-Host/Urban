package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdAndIsReadFalseOrderBySentAtDesc(UUID userId);

    List<Notification> findByUserIdOrderBySentAtDesc(UUID userId);

    List<Notification> findByUserIdAndTypeAndIsReadFalseOrderBySentAtDesc(
            UUID userId, String type);

    long countByUserIdAndIsReadFalse(UUID userId);

    // ── Mark ALL notifications as read for a user ──
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadForUser(@Param("userId") UUID userId);

    boolean existsByUserIdAndReferenceIdAndType(UUID userId, Long referenceId, String type);

    List<Notification> findByReferenceIdAndType(Long referenceId, String type);
}
