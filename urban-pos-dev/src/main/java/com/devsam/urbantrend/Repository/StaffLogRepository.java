package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.StaffLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StaffLogRepository extends JpaRepository<StaffLog, UUID> {

    List<StaffLog> findByStaffId(String staffId);

    List<StaffLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
}
