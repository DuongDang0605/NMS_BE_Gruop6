package com.mvc.nms_be.service;

import com.mvc.nms_be.entity.SystemSetting;
import com.mvc.nms_be.repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc/ghi chế độ kiểm duyệt qua SystemSetting để thay đổi của Admin còn hiệu lực sau restart.
 */
@Service
public class ModerationSettingService {

    private static final String SETTING_KEY = "COMMENT_MODERATION_MODE";

    private final SystemSettingRepository settingRepository;
    private final boolean defaultPreModeration;

    public ModerationSettingService(
            SystemSettingRepository settingRepository,
            @Value("${app.moderation.pre-moderation:false}") boolean defaultPreModeration) {
        this.settingRepository = settingRepository;
        this.defaultPreModeration = defaultPreModeration;
    }

    @Transactional(readOnly = true)
    public boolean isGlobalPreModeration() {
        return settingRepository.findBySettingKey(SETTING_KEY)
                .map(setting -> parseBoolean(setting.getSettingValue()))
                .orElse(defaultPreModeration);
    }

    @Transactional
    public void setGlobalPreModeration(boolean enabled) {
        SystemSetting setting = settingRepository.findBySettingKey(SETTING_KEY)
                .orElseGet(() -> {
                    SystemSetting created = new SystemSetting();
                    created.setSettingKey(SETTING_KEY);
                    created.setDescription("Chế độ kiểm duyệt bình luận: PRE_MODERATION hoặc POST_MODERATION");
                    return created;
                });
        setting.setSettingValue(enabled ? "PRE_MODERATION" : "POST_MODERATION");
        settingRepository.save(setting);
    }

    private boolean parseBoolean(String value) {
        if ("PRE_MODERATION".equalsIgnoreCase(value)) {
            return true;
        }
        if ("POST_MODERATION".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalStateException(
                "Giá trị cấu hình " + SETTING_KEY
                        + " phải là PRE_MODERATION hoặc POST_MODERATION");
    }
}