package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.response.PublicArticleDto;
import java.util.List;
import java.util.Map;

public interface PublicPortalService {

    /**
     * Lấy danh sách bài đã xuất bản (newsStatus=1), hỗ trợ lọc keyword/category/tag và phân trang.
     * Không yêu cầu đăng nhập.
     */
    Map<String, Object> getPublishedArticles(String keyword, Integer categoryId, Integer tagId,
                                              int page, int size);

    /**
     * Lấy chi tiết 1 bài đã xuất bản kèm viewCount, likeCount và cờ isLikedByCurrentUser.
     * Không yêu cầu đăng nhập (isLikedByCurrentUser = false nếu chưa đăng nhập).
     */
    PublicArticleDto getArticleDetail(Integer articleId);
}
