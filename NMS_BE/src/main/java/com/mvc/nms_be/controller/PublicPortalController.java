package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.response.PublicArticleDto;
import com.mvc.nms_be.service.PublicPortalService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
public class PublicPortalController {

    private final PublicPortalService publicPortalService;

    /**
     * GET /api/v1/portal/articles
     * Danh sách bài đã xuất bản, hỗ trợ lọc và phân trang. Không cần đăng nhập.
     */
    @GetMapping("/articles")
    public Map<String, Object> getPublishedArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return publicPortalService.getPublishedArticles(keyword, categoryId, tagId, page, size);
    }

    /**
     * GET /api/v1/portal/articles/{id}
     * Chi tiết bài viết kèm viewCount, likeCount, isLikedByCurrentUser. Không cần đăng nhập.
     */
    @GetMapping("/articles/{id}")
    public PublicArticleDto getArticleDetail(@PathVariable Integer id) {
        return publicPortalService.getArticleDetail(id);
    }
}
