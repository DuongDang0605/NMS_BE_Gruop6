package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.response.NewsArticleResponseDto;
import com.mvc.nms_be.service.NewsArticleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/portal/news", "/api/v1/news/public"})
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
public class PublicNewsController {
    private final NewsArticleService news;

    @GetMapping
    public List<NewsArticleResponseDto> list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId, @RequestParam(required = false) Integer tagId) {
        return news.publishedArticles(keyword, categoryId, tagId);
    }

    @GetMapping("/{id}")
    public NewsArticleResponseDto detail(@PathVariable Integer id) { return news.publishedArticle(id); }
}
