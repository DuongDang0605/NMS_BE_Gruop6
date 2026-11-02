package com.mvc.nms_be.dto.response;

import java.time.LocalDateTime;

public record ArticleReviewNoteResponseDto(
        Integer noteId,
        Integer articleId,
        Integer editorId,
        String editorName,
        String feedbackContent,
        String actionType,
        LocalDateTime createdDate
) {}

