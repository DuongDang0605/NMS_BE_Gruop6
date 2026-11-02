package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.request.NewsArticleRequestDto;
import com.mvc.nms_be.dto.response.ArticleReviewNoteResponseDto;
import com.mvc.nms_be.dto.response.NewsArticleResponseDto;
import com.mvc.nms_be.service.AuthorArticleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/author/articles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CONTRIBUTOR', 'ADMIN')")
public class AuthorArticleController {

    private final AuthorArticleService authorArticleService;

    @GetMapping
    public ResponseEntity<Page<NewsArticleResponseDto>> getMyArticles(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(authorArticleService.getMyArticles(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsArticleResponseDto> getArticleById(@PathVariable Integer id) {
        return ResponseEntity.ok(authorArticleService.getArticleById(id));
    }

    @PostMapping
    public ResponseEntity<NewsArticleResponseDto> createDraft(@Valid @RequestBody NewsArticleRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authorArticleService.createDraft(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NewsArticleResponseDto> updateDraft(
            @PathVariable Integer id,
            @Valid @RequestBody NewsArticleRequestDto request) {
        return ResponseEntity.ok(authorArticleService.updateDraft(id, request));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<NewsArticleResponseDto> submitForReview(@PathVariable Integer id) {
        return ResponseEntity.ok(authorArticleService.submitForReview(id));
    }

    @GetMapping("/{id}/feedback")
    public ResponseEntity<List<ArticleReviewNoteResponseDto>> getFeedbackNotes(@PathVariable Integer id) {
        return ResponseEntity.ok(authorArticleService.getFeedbackNotes(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDraft(@PathVariable Integer id) {
        authorArticleService.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }
}

