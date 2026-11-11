package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.SalaryAdvance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, UUID> {
    boolean existsByRequisition_Id(UUID requisitionId);
}

