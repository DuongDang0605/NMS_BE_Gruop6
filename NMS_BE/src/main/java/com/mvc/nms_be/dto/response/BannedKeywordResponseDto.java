package com.mvc.nms_be.dto.response;


import java.time.LocalDateTime;

public record BannedKeywordResponseDto(
        Integer keywordId,
        String word,
        String severity,
        Boolean isActive,
        LocalDateTime createdDate
) {
}