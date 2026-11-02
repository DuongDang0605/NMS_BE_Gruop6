package com.mvc.nms_be.dto.response;


import java.time.LocalDateTime;

/**
 * Dữ liệu một bình luận trả về sau khi đăng, hoặc dùng trong danh sách kiểm duyệt.
 */
public record CommentResponseDto(
        Integer commentId,
        Integer newsArticleId,
        Integer accountId,
        String authorName,
        Integer parentCommentId,
        String content,
        String status,
        Boolean isFlaggedProfanity,
        Integer reportCount,
        LocalDateTime createdDate,
        LocalDateTime updatedDate
) {
}
