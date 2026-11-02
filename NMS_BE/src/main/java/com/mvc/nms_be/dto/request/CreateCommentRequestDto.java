package com.mvc.nms_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Yêu cầu đăng bình luận mới hoặc trả lời một bình luận (parentCommentId != null).
 */
public record CreateCommentRequestDto(
        @NotNull(message = "newsArticleId không được để trống")
        Integer newsArticleId,

        Integer parentCommentId,

        @NotBlank(message = "Nội dung bình luận không được để trống")
        @Size(max = 2000, message = "Nội dung bình luận tối đa 2000 ký tự")
        String content
) {
}
