package com.mvc.nms_be.dto.response;

/**
 * Cấu hình kiểm duyệt hiện tại, trả về từ GET và PUT /api/v1/moderation/settings.
 */
public record ModerationSettingResponseDto(boolean preModeration) {
}