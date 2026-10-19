package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.request.NewsTagRequestDto;
import com.mvc.nms_be.dto.response.NewsTagResponseDto;
import com.mvc.nms_be.service.NewsTagService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news-tags")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NewsTagController {

    private final NewsTagService newsTagService;

    @GetMapping
    public ResponseEntity<List<NewsTagResponseDto>> getAllNewsTags() {
        return ResponseEntity.ok(newsTagService.getAllNewsTags());
    }

    @GetMapping("/article/{articleId}")
    public ResponseEntity<List<NewsTagResponseDto>> getTagsByArticle(@PathVariable Integer articleId) {
        return ResponseEntity.ok(newsTagService.getTagsByArticleId(articleId));
    }

    @GetMapping("/tag/{tagId}")
    public ResponseEntity<List<NewsTagResponseDto>> getArticlesByTag(@PathVariable Integer tagId) {
        return ResponseEntity.ok(newsTagService.getArticlesByTagId(tagId));
    }

    @PostMapping
    public ResponseEntity<NewsTagResponseDto> addTagToArticle(@RequestBody NewsTagRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(newsTagService.addTagToArticle(request));
    }

    @DeleteMapping
    public ResponseEntity<Void> removeTagFromArticle(
            @RequestParam Integer articleId,
            @RequestParam Integer tagId) {
        newsTagService.removeTagFromArticle(articleId, tagId);
        return ResponseEntity.noContent().build();
    }
}