package com.mvc.nms_be.dto.response;

import java.time.LocalDateTime;

/**
 * Phiếu báo cáo hiển thị trong bàn làm việc của kiểm duyệt viên.
 */
public record CommentReportResponseDto(
        Integer reportId,
        Integer commentId,
        String commentContent,
        String commentStatus,
        Integer commentReportCount,
        Integer reporterId,
        String reporterName,
        String reason,
        String status,
        Integer resolvedById,
        LocalDateTime createdDate,
        LocalDateTime resolvedDate
) {
}
