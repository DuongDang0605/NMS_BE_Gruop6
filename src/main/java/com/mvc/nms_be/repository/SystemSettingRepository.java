package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.SystemSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Integer> {
 
}
