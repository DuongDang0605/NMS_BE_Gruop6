package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.request.NewsArticleRequestDto;
import com.mvc.nms_be.dto.response.NewsArticleResponseDto;
import com.mvc.nms_be.service.NewsArticleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NewsArticleController {

    private final NewsArticleService newsArticleService;

    @GetMapping
    public ResponseEntity<List<NewsArticleResponseDto>> getAllArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer newsStatus,
            @RequestParam(required = false) Integer tagId) {
        if (keyword != null || categoryId != null || newsStatus != null || tagId != null) {
            return ResponseEntity.ok(newsArticleService.searchArticles(keyword, categoryId, newsStatus, tagId));
        }
        return ResponseEntity.ok(newsArticleService.getAllArticles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsArticleResponseDto> getArticleById(@PathVariable Integer id) {
        return ResponseEntity.ok(newsArticleService.getArticleById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<NewsArticleResponseDto>> searchArticles(@RequestParam String keyword) {
        return ResponseEntity.ok(newsArticleService.searchArticles(keyword, null, null, null));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<NewsArticleResponseDto>> getByCategory(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(newsArticleService.getArticlesByCategory(categoryId));
    }

    @GetMapping("/tag/{tagId}")
    public ResponseEntity<List<NewsArticleResponseDto>> getByTag(@PathVariable Integer tagId) {
        return ResponseEntity.ok(newsArticleService.getArticlesByTag(tagId));
    }

    @PostMapping
    public ResponseEntity<NewsArticleResponseDto> createArticle(@RequestBody NewsArticleRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(newsArticleService.createArticle(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NewsArticleResponseDto> updateArticle(
            @PathVariable Integer id,
            @RequestBody NewsArticleRequestDto request) {
        return ResponseEntity.ok(newsArticleService.updateArticle(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<NewsArticleResponseDto> updateStatus(
            @PathVariable Integer id,
            @RequestParam Integer status) {
        return ResponseEntity.ok(newsArticleService.updateArticleStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Integer id) {
        newsArticleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}