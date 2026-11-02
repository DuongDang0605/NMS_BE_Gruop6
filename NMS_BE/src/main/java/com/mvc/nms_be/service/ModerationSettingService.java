package com.mvc.nms_be.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cấu hình chế độ kiểm duyệt toàn trang (PRE_MODERATION / POST_MODERATION).
 * <p>
 * Giá trị khởi tạo lấy từ application.properties: app.moderation.pre-moderation=true|false
 * (mặc định false = POST_MODERATION). Admin có thể đổi lúc chạy qua API, nhưng giá trị
 * chỉ nằm trong bộ nhớ nên sẽ quay về giá trị cấu hình khi khởi động lại ứng dụng.
 */
@Service
public class ModerationSettingService {

    private final AtomicBoolean globalPreModeration;

    public ModerationSettingService(@Value("${app.moderation.pre-moderation:false}") boolean initial) {
        this.globalPreModeration = new AtomicBoolean(initial);
    }

    public boolean isGlobalPreModeration() {
        return globalPreModeration.get();
    }

    public void setGlobalPreModeration(boolean enabled) {
        globalPreModeration.set(enabled);
    }
}