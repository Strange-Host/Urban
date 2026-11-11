package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.LogoutCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LogoutCheckRepository extends JpaRepository<LogoutCheck, UUID> {
}