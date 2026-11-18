package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    @Modifying
    @Transactional
    @Query("UPDATE AuthSession s SET s.revokedAt = :now WHERE s.userId = :userId AND s.revokedAt IS NULL")
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM AuthSession s WHERE s.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
