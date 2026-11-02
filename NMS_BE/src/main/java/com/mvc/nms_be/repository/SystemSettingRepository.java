package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.SystemSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Integer> {

    // Tìm cấu hình theo key, ví dụ findBySettingKey("SiteName")
    Optional<SystemSetting> findBySettingKey(String settingKey);

    // Key đã tồn tại chưa (dùng để chống trùng key)
    boolean existsBySettingKey(String settingKey);
}
