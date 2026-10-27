package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.response.ReactionResponseDto;
import com.mvc.nms_be.dto.response.ViewCountResponseDto;

public interface EngagementService {


     // toggle like/unlike bài viết cho user đang đăng nhập
     // nếu chưa like → tạo record và tăng likeCount
     // nếu đã like → xóa record và giảm likeCount

    ReactionResponseDto toggleReaction(Integer articleId);

    // tăng lượt xem bài viết 1 đơn vị (atomic UPDATE trực tiếp trên DB)
    // trả về viewCount mới nhất
    ViewCountResponseDto incrementViewCount(Integer articleId);
}
