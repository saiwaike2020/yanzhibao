package com.crm.repository;

import com.crm.entity.SystemSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 系统参数表仓储。
 */
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findBySettingKey(String settingKey);
}
