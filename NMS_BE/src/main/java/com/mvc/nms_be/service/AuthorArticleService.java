package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.NewsArticleRequestDto;
import com.mvc.nms_be.dto.response.ArticleAuditLogResponseDto;
import com.mvc.nms_be.dto.response.ArticleReviewNoteResponseDto;
import com.mvc.nms_be.dto.response.NewsArticleResponseDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthorArticleService {

    Page<NewsArticleResponseDto> getMyArticles(String status, Pageable pageable);

    NewsArticleResponseDto getArticleById(Integer articleId);

    NewsArticleResponseDto createDraft(NewsArticleRequestDto request);

    NewsArticleResponseDto updateDraft(Integer articleId, NewsArticleRequestDto request);

    NewsArticleResponseDto submitForReview(Integer articleId);

    List<ArticleReviewNoteResponseDto> getFeedbackNotes(Integer articleId);

    List<ArticleAuditLogResponseDto> getArticleAuditTrail(Integer articleId);

    void deleteDraft(Integer articleId);
}

