package com.mvc.nms_be.dto.response;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nút trong cây bình luận đa cấp. Danh sách replies là ArrayList có thể thay đổi
 * để thuật toán dựng cây gắn con vào cha.
 */
public record CommentNodeDto(
        Integer commentId,
        Integer accountId,
        String authorName,
        Integer parentCommentId,
        String content,
        String status,
        LocalDateTime createdDate,
        List<CommentNodeDto> replies
) {
    public CommentNodeDto {
        if (replies == null) {
            replies = new ArrayList<>();
        }
    }
}