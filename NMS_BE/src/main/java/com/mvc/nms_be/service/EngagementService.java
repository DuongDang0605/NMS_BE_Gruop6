package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.response.ReactionResponseDto;
import com.mvc.nms_be.dto.response.ViewCountResponseDto;

public interface EngagementService {

    /**
     * Toggle like/unlike bài viết cho user đang đăng nhập.
     * Nếu chưa like → tạo record và tăng likeCount.
     * Nếu đã like → xóa record và giảm likeCount.
     */
    ReactionResponseDto toggleReaction(Integer articleId);

    /**
     * Tăng lượt xem bài viết 1 đơn vị (atomic UPDATE trực tiếp trên DB).
     * Trả về viewCount mới nhất.
     */
    ViewCountResponseDto incrementViewCount(Integer articleId);
}
