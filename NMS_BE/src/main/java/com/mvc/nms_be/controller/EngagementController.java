package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.response.ReactionResponseDto;
import com.mvc.nms_be.dto.response.ViewCountResponseDto;
import com.mvc.nms_be.service.EngagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news/{id}")
@RequiredArgsConstructor
public class EngagementController {

    private final EngagementService engagementService;

    // Toggle like/unlike bài viết. Yêu cầu đăng nhập
    @PostMapping("/reactions")
    public ReactionResponseDto toggleReaction(@PathVariable Integer id) {
        return engagementService.toggleReaction(id);
    }


     // Tăng lượt xem 1 đơn vị gọi mỗi khi người dùng mở trang chi tiết bài viết
    @PostMapping("/views")
    public ViewCountResponseDto incrementViewCount(@PathVariable Integer id) {
        return engagementService.incrementViewCount(id);
    }
}
