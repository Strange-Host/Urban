package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
