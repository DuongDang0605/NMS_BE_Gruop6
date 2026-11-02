package com.mvc.nms_be.dto.request;

/**
 * Body của PUT /api/v1/moderation/settings.
 * preModeration = true: mọi bình luận mới đều phải chờ duyệt (PRE_MODERATION).
 * preModeration = false: chỉ bình luận của tài khoản chưa xác minh hoặc dính từ FLAG phải chờ duyệt.
 */
public record ModerationSettingRequestDto(boolean preModeration) {
}