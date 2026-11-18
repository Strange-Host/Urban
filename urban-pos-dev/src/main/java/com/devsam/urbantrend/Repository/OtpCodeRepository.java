package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.OtpCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, String> {

    /** Row-locks the code so parallel guesses are counted one after another, never together. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OtpCode o WHERE o.phoneNumber = :phone")
    Optional<OtpCode> lockByPhoneNumber(@Param("phone") String phone);

    @Modifying
    @Transactional
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
