package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.FeedbackRequestDto;
import com.mvc.nms_be.dto.request.SchedulePublicationRequestDto;
import com.mvc.nms_be.dto.response.ArticleAuditLogResponseDto;
import com.mvc.nms_be.dto.response.NewsArticleResponseDto;
import com.mvc.nms_be.entity.ArticleReviewNote;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.exception.ResourceNotFoundException;
import com.mvc.nms_be.mapper.NewsArticleMapper;
import com.mvc.nms_be.repository.ArticleReviewNoteRepository;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.security.SecurityUtils;
import com.mvc.nms_be.service.ArticleAuditLogService;
import com.mvc.nms_be.service.EditorialService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditorialServiceImpl implements EditorialService {

    private final NewsArticleRepository newsArticleRepository;
    private final ArticleReviewNoteRepository reviewNoteRepository;
    private final ArticleAuditLogService auditLogService;
    private final NewsArticleMapper newsArticleMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NewsArticleResponseDto> getReviewQueue(Pageable pageable) {
        return newsArticleRepository.findByNewsStatus(NewsArticle.STATUS_PENDING_REVIEW, pageable)
                .map(newsArticleMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public NewsArticleResponseDto getArticleDetail(Integer articleId) {
        NewsArticle article = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại: " + articleId));
        return newsArticleMapper.toResponseDto(article);
    }

    @Override
    @Transactional
    public void requestChanges(Integer articleId, FeedbackRequestDto feedbackDto) {
        if (feedbackDto == null || feedbackDto.feedbackContent() == null || feedbackDto.feedbackContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Yêu cầu phản hồi bắt buộc phải có nội dung ghi chú giải thích lý do");
        }

        NewsArticle article = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại: " + articleId));

        if (!NewsArticle.STATUS_PENDING_REVIEW.equalsIgnoreCase(article.getNewsStatus()) && !SecurityUtils.isAdmin()) {
            throw new IllegalStateException("Chỉ có thể yêu cầu sửa đổi đối với bài viết đang chờ duyệt (PENDING_REVIEW). Trạng thái hiện tại: " + article.getNewsStatus());
        }

        String oldStatus = article.getNewsStatus();
        article.setNewsStatus(NewsArticle.STATUS_CHANGES_REQUESTED);
        article.setModifiedDate(LocalDateTime.now(ZoneOffset.UTC));
        SystemAccount editor = SecurityUtils.getCurrentUser().getAccount();
        article.setUpdatedBy(editor);
        newsArticleRepository.save(article);

        ArticleReviewNote note = new ArticleReviewNote();
        note.setNewsArticle(article);
        note.setEditor(editor);
        note.setFeedbackContent(feedbackDto.feedbackContent().trim());
        note.setActionType("CHANGES_REQUESTED");
        note.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        reviewNoteRepository.save(note);

        auditLogService.logAction(
                articleId,
                editor.getAccountId(),
                editor.getAccountName(),
                "REQUEST_CHANGES",
                oldStatus,
                NewsArticle.STATUS_CHANGES_REQUESTED,
                feedbackDto.feedbackContent().trim()
        );
    }

    @Override
    @Transactional
    public NewsArticleResponseDto approveAndPublishNow(Integer articleId) {
        NewsArticle article = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại: " + articleId));

        if (!NewsArticle.STATUS_PENDING_REVIEW.equalsIgnoreCase(article.getNewsStatus()) && !SecurityUtils.isAdmin()) {
            throw new IllegalStateException("Chỉ bài viết ở PENDING_REVIEW mới có thể được duyệt xuất bản ngay. Trạng thái hiện tại: " + article.getNewsStatus());
        }

        String oldStatus = article.getNewsStatus();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        article.setNewsStatus(NewsArticle.STATUS_PUBLISHED);
        article.setPublishedDate(now);
        article.setModifiedDate(now);
        SystemAccount editor = SecurityUtils.getCurrentUser().getAccount();
        article.setUpdatedBy(editor);
        NewsArticle saved = newsArticleRepository.save(article);

        auditLogService.logAction(
                articleId,
                editor.getAccountId(),
                editor.getAccountName(),
                "PUBLISH",
                oldStatus,
                NewsArticle.STATUS_PUBLISHED,
                "Biên tập viên đã phê duyệt và xuất bản ngay"
        );

        return newsArticleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public NewsArticleResponseDto approveAndSchedule(Integer articleId, SchedulePublicationRequestDto request) {
        if (request == null || request.scheduledPublishDate() == null) {
            throw new IllegalArgumentException("Thời gian hẹn giờ xuất bản không được để trống");
        }

        LocalDateTime scheduleTime = request.scheduledPublishDate();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (scheduleTime.isBefore(now.plusMinutes(1))) {
            throw new IllegalArgumentException("Thời gian hẹn giờ xuất bản phải ở tương lai ít nhất 1 phút");
        }

        NewsArticle article = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại: " + articleId));

        if (!NewsArticle.STATUS_PENDING_REVIEW.equalsIgnoreCase(article.getNewsStatus()) && !SecurityUtils.isAdmin()) {
            throw new IllegalStateException("Chỉ bài viết ở PENDING_REVIEW mới có thể được duyệt và lên lịch. Trạng thái hiện tại: " + article.getNewsStatus());
        }

        String oldStatus = article.getNewsStatus();
        article.setNewsStatus(NewsArticle.STATUS_SCHEDULED);
        article.setScheduledPublishDate(scheduleTime);
        article.setModifiedDate(now);
        SystemAccount editor = SecurityUtils.getCurrentUser().getAccount();
        article.setUpdatedBy(editor);
        NewsArticle saved = newsArticleRepository.save(article);

        auditLogService.logAction(
                articleId,
                editor.getAccountId(),
                editor.getAccountName(),
                "SCHEDULE",
                oldStatus,
                NewsArticle.STATUS_SCHEDULED,
                "Đã lên lịch xuất bản tự động vào: " + scheduleTime
        );

        return newsArticleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public NewsArticleResponseDto archiveArticle(Integer articleId) {
        NewsArticle article = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại: " + articleId));

        if (!NewsArticle.STATUS_PUBLISHED.equalsIgnoreCase(article.getNewsStatus()) && !SecurityUtils.isAdmin()) {
            throw new IllegalStateException("Chỉ bài viết đã xuất bản (PUBLISHED) mới có thể gỡ lưu trữ. Trạng thái hiện tại: " + article.getNewsStatus());
        }

        String oldStatus = article.getNewsStatus();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        article.setNewsStatus(NewsArticle.STATUS_ARCHIVED);
        article.setModifiedDate(now);
        SystemAccount editor = SecurityUtils.getCurrentUser().getAccount();
        article.setUpdatedBy(editor);
        NewsArticle saved = newsArticleRepository.save(article);

        auditLogService.logAction(
                articleId,
                editor.getAccountId(),
                editor.getAccountName(),
                "ARCHIVE",
                oldStatus,
                NewsArticle.STATUS_ARCHIVED,
                "Đã gỡ bài viết vào kho lưu trữ"
        );

        return newsArticleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public NewsArticleResponseDto restoreArticle(Integer articleId) {
        NewsArticle article = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại: " + articleId));

        if (!NewsArticle.STATUS_ARCHIVED.equalsIgnoreCase(article.getNewsStatus()) && !SecurityUtils.isAdmin()) {
            throw new IllegalStateException("Chỉ bài viết trong kho lưu trữ (ARCHIVED) mới có thể khôi phục xuất bản. Trạng thái hiện tại: " + article.getNewsStatus());
        }

        String oldStatus = article.getNewsStatus();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        article.setNewsStatus(NewsArticle.STATUS_PUBLISHED);
        article.setModifiedDate(now);
        SystemAccount editor = SecurityUtils.getCurrentUser().getAccount();
        article.setUpdatedBy(editor);
        NewsArticle saved = newsArticleRepository.save(article);

        auditLogService.logAction(
                articleId,
                editor.getAccountId(),
                editor.getAccountName(),
                "RESTORE",
                oldStatus,
                NewsArticle.STATUS_PUBLISHED,
                "Khôi phục bài viết xuất bản lại công khai"
        );

        return newsArticleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleAuditLogResponseDto> getArticleAuditTrail(Integer articleId) {
        return auditLogService.getAuditTrail(articleId);
    }
}

