package com.mvc.nms_be.dto.response;

import java.time.LocalDateTime;

public record ArticleAuditLogResponseDto(
        Long logId,
        Integer newsArticleId,
        Integer actorId,
        String actorName,
        String action,
        String fromStatus,
        String toStatus,
        String notes,
        LocalDateTime createdAt
) {}

