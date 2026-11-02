package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.NewsArticleRequestDto;
import com.mvc.nms_be.dto.response.ArticleReviewNoteResponseDto;
import com.mvc.nms_be.dto.response.NewsArticleResponseDto;
import com.mvc.nms_be.entity.Category;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.entity.Tag;
import com.mvc.nms_be.exception.ForbiddenException;
import com.mvc.nms_be.exception.ResourceNotFoundException;
import com.mvc.nms_be.mapper.NewsArticleMapper;
import com.mvc.nms_be.repository.ArticleAuditLogRepository;
import com.mvc.nms_be.repository.ArticleReviewNoteRepository;
import com.mvc.nms_be.repository.CategoryRepository;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.repository.NewsTagRepository;
import com.mvc.nms_be.repository.TagRepository;
import com.mvc.nms_be.security.SecurityUtils;
import com.mvc.nms_be.service.ArticleAuditLogService;
import com.mvc.nms_be.service.AuthorArticleService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthorArticleServiceImpl implements AuthorArticleService {

    private final NewsArticleRepository newsArticleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final NewsTagRepository newsTagRepository;
    private final ArticleReviewNoteRepository reviewNoteRepository;
    private final ArticleAuditLogRepository auditLogRepository;
    private final ArticleAuditLogService auditLogService;
    private final NewsArticleMapper newsArticleMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NewsArticleResponseDto> getMyArticles(String status, Pageable pageable) {
        Integer currentUserId = SecurityUtils.getCurrentAccountId();
        Page<NewsArticle> page;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            page = newsArticleRepository.findByCreatedBy_AccountIdAndNewsStatus(
                    currentUserId, status.trim().toUpperCase(), pageable);
        } else {
            page = newsArticleRepository.findByCreatedBy_AccountId(currentUserId, pageable);
        }
        return page.map(newsArticleMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public NewsArticleResponseDto getArticleById(Integer articleId) {
        NewsArticle article = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết: " + articleId));

        validateOwnership(article);
        return newsArticleMapper.toResponseDto(article);
    }

    @Override
    @Transactional
    public NewsArticleResponseDto createDraft(NewsArticleRequestDto request) {
        if (request.getNewsTitle() == null || request.getNewsTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề bài viết không được để trống");
        }
        if (request.getNewsContent() == null || request.getNewsContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung bài viết không được để trống");
        }
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Danh mục không được để trống");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại: " + request.getCategoryId()));

        SystemAccount currentAccount = SecurityUtils.getCurrentUser().getAccount();
        Integer currentUserId = currentAccount.getAccountId();
        String actorName = currentAccount.getAccountName();

        request.setNewsStatus(NewsArticle.STATUS_DRAFT);

        List<Tag> tags = new ArrayList<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = tagRepository.findAllById(request.getTagIds());
        }

        NewsArticle article = newsArticleMapper.toEntity(request, category, currentAccount, tags);
        article.setNewsStatus(NewsArticle.STATUS_DRAFT);
        article.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        NewsArticle saved = newsArticleRepository.save(article);

        auditLogService.logAction(
                saved.getNewsArticleId(),
                currentUserId,
                actorName,
                "CREATE_DRAFT",
                null,
                NewsArticle.STATUS_DRAFT,
                "Tác giả tạo mới bản nháp"
        );

        return newsArticleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public NewsArticleResponseDto updateDraft(Integer articleId, NewsArticleRequestDto request) {
        NewsArticle article = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết: " + articleId));

        validateOwnership(article);

        String currentStatus = article.getNewsStatus();
        if (!NewsArticle.STATUS_DRAFT.equalsIgnoreCase(currentStatus)
                && !NewsArticle.STATUS_CHANGES_REQUESTED.equalsIgnoreCase(currentStatus)
                && !SecurityUtils.isAdmin()) {
            throw new ForbiddenException("Bài viết đang trong quy trình xét duyệt hoặc đã xuất bản, không được phép chỉnh sửa trực tiếp (Trạng thái hiện tại: " + currentStatus + ")");
        }

        if (request.getNewsTitle() != null && request.getNewsTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề bài viết không được để trống");
        }
        if (request.getNewsContent() != null && request.getNewsContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung bài viết không được để trống");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại: " + request.getCategoryId()));
        }

        SystemAccount updatedBy = SecurityUtils.getCurrentUser().getAccount();
        List<Tag> tags = null;
        if (request.getTagIds() != null) {
            tags = request.getTagIds().isEmpty() ? new ArrayList<>() : tagRepository.findAllById(request.getTagIds());
        }

        newsArticleMapper.updateEntity(article, request, category, updatedBy, tags);
        article.setModifiedDate(LocalDateTime.now(ZoneOffset.UTC));
        NewsArticle updated = newsArticleRepository.save(article);

        auditLogService.logAction(
                articleId,
                updatedBy.getAccountId(),
                updatedBy.getAccountName(),
                "UPDATE_DRAFT",
                currentStatus,
                article.getNewsStatus(),
                "Tác giả cập nhật nội dung bài viết"
        );

        return newsArticleMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public NewsArticleResponseDto submitForReview(Integer articleId) {
        NewsArticle article = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết: " + articleId));

        validateOwnership(article);

        String currentStatus = article.getNewsStatus();
        if (!NewsArticle.STATUS_DRAFT.equalsIgnoreCase(currentStatus)
                && !NewsArticle.STATUS_CHANGES_REQUESTED.equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Chỉ có thể nộp duyệt bài ở trạng thái DRAFT hoặc CHANGES_REQUESTED. Trạng thái hiện tại: " + currentStatus);
        }

        article.setNewsStatus(NewsArticle.STATUS_PENDING_REVIEW);
        article.setModifiedDate(LocalDateTime.now(ZoneOffset.UTC));
        NewsArticle saved = newsArticleRepository.save(article);

        SystemAccount currentUser = SecurityUtils.getCurrentUser().getAccount();
        auditLogService.logAction(
                article.getNewsArticleId(),
                currentUser.getAccountId(),
                currentUser.getAccountName(),
                "SUBMIT_REVIEW",
                currentStatus,
                NewsArticle.STATUS_PENDING_REVIEW,
                "Tác giả đã nộp bài chờ ban biên tập phê duyệt"
        );

        return newsArticleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleReviewNoteResponseDto> getFeedbackNotes(Integer articleId) {
        NewsArticle article = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết: " + articleId));

        validateOwnership(article);

        return reviewNoteRepository.findByNewsArticleIdWithEditorOrderByCreatedDateDesc(articleId).stream()
                .map(note -> new ArticleReviewNoteResponseDto(
                        note.getNoteId(),
                        note.getNewsArticle().getNewsArticleId(),
                        note.getEditor().getAccountId(),
                        note.getEditor().getAccountName(),
                        note.getFeedbackContent(),
                        note.getActionType(),
                        note.getCreatedDate()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void deleteDraft(Integer articleId) {
        NewsArticle article = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết: " + articleId));

        validateOwnership(article);

        if (!NewsArticle.STATUS_DRAFT.equalsIgnoreCase(article.getNewsStatus()) && !SecurityUtils.isAdmin()) {
            throw new ForbiddenException("Chỉ có thể xóa bản nháp ở trạng thái DRAFT");
        }

        newsTagRepository.deleteByArticleId(articleId);
        reviewNoteRepository.deleteByNewsArticle_NewsArticleId(articleId);
        auditLogRepository.deleteByNewsArticleId(articleId);
        newsArticleRepository.delete(article);
    }

    private void validateOwnership(NewsArticle article) {
        Integer currentUserId = SecurityUtils.getCurrentAccountId();
        if (!SecurityUtils.isAdmin()
                && (article.getCreatedBy() == null || !article.getCreatedBy().getAccountId().equals(currentUserId))) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên bài viết của người khác");
        }
    }
}

