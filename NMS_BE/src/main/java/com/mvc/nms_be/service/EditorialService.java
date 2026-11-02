package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.FeedbackRequestDto;
import com.mvc.nms_be.dto.request.SchedulePublicationRequestDto;
import com.mvc.nms_be.dto.response.ArticleAuditLogResponseDto;
import com.mvc.nms_be.dto.response.NewsArticleResponseDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EditorialService {

    Page<NewsArticleResponseDto> getReviewQueue(Pageable pageable);

    NewsArticleResponseDto getArticleDetail(Integer articleId);

    void requestChanges(Integer articleId, FeedbackRequestDto feedbackDto);

    NewsArticleResponseDto approveAndPublishNow(Integer articleId);

    NewsArticleResponseDto approveAndSchedule(Integer articleId, SchedulePublicationRequestDto request);

    NewsArticleResponseDto archiveArticle(Integer articleId);

    NewsArticleResponseDto restoreArticle(Integer articleId);

    List<ArticleAuditLogResponseDto> getArticleAuditTrail(Integer articleId);
}

