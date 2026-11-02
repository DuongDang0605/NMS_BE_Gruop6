package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.response.ArticleAuditLogResponseDto;
import java.util.List;

public interface ArticleAuditLogService {

    void logAction(Integer articleId, Integer actorId, String actorName,
                   String action, String fromStatus, String toStatus, String notes);

    List<ArticleAuditLogResponseDto> getAuditTrail(Integer articleId);
}

