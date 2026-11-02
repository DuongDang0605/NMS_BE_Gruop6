package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.NewsArticleRequestDto;
import com.mvc.nms_be.dto.response.NewsArticleResponseDto;
import com.mvc.nms_be.entity.Category;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.entity.Tag;
import com.mvc.nms_be.mapper.NewsArticleMapper;
import com.mvc.nms_be.repository.CategoryRepository;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.repository.NewsTagRepository;
import com.mvc.nms_be.security.SecurityUtils;
import com.mvc.nms_be.repository.TagRepository;
import com.mvc.nms_be.service.ArticleAuditLogService;
import com.mvc.nms_be.service.NewsArticleService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CONTRIBUTOR', 'EDITOR')")
public class NewsArticleServiceImpl implements NewsArticleService {

    private static final List<String> VALID_STATUSES = List.of(
            NewsArticle.STATUS_DRAFT,
            NewsArticle.STATUS_PENDING_REVIEW,
            NewsArticle.STATUS_CHANGES_REQUESTED,
            NewsArticle.STATUS_APPROVED,
            NewsArticle.STATUS_SCHEDULED,
            NewsArticle.STATUS_PUBLISHED,
            NewsArticle.STATUS_ARCHIVED
    );

    private final NewsArticleRepository newsArticleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final NewsTagRepository newsTagRepository;
    private final NewsArticleMapper newsArticleMapper;
    private final ArticleAuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<NewsArticleResponseDto> getAllArticles() {
        return newsArticleRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDate")).stream()
                .filter(this::canRead).map(newsArticleMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsArticleResponseDto> searchArticles(String keyword, Integer categoryId, String newsStatus, Integer tagId) {
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String normalizedStatus = null;
        if (newsStatus != null && !newsStatus.trim().isEmpty() && !"ALL".equalsIgnoreCase(newsStatus.trim())) {
            String s = newsStatus.trim().toUpperCase();
            if ("1".equals(s) || "ACTIVE".equals(s)) {
                normalizedStatus = NewsArticle.STATUS_PUBLISHED;
            } else if ("0".equals(s) || "INACTIVE".equals(s)) {
                normalizedStatus = NewsArticle.STATUS_ARCHIVED;
            } else {
                normalizedStatus = s;
            }
        }
        return newsArticleRepository.searchArticles(trimmedKeyword, categoryId, normalizedStatus, tagId).stream()
                .filter(this::canRead).map(newsArticleMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NewsArticleResponseDto getArticleById(Integer id) {
        NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News article not found with id: " + id));
        if (!canRead(article)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "News article not found");
        return newsArticleMapper.toResponseDto(article);
    }

    @Override
    @Transactional
    public NewsArticleResponseDto createArticle(NewsArticleRequestDto request) {
        if (request.getNewsTitle() == null || request.getNewsTitle().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "News title cannot be empty");
        }
        if (request.getNewsContent() == null || request.getNewsContent().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "News content cannot be empty");
        }
        if (request.getCategoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CategoryId is required");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found with id: " + request.getCategoryId()));

        SystemAccount createdBy = SecurityUtils.getCurrentUser().getAccount();
        boolean canPublish = SecurityUtils.isAdmin() || SecurityUtils.hasRole("EDITOR");

        String requestedStatus = request.getNewsStatus();
        if (requestedStatus != null && !requestedStatus.isBlank()) {
            requestedStatus = requestedStatus.trim().toUpperCase();
            if ("1".equals(requestedStatus) || "ACTIVE".equals(requestedStatus)) {
                requestedStatus = NewsArticle.STATUS_PUBLISHED;
            } else if ("0".equals(requestedStatus) || "INACTIVE".equals(requestedStatus)) {
                requestedStatus = NewsArticle.STATUS_DRAFT;
            }
        } else {
            requestedStatus = NewsArticle.STATUS_DRAFT;
        }

        if (!canPublish && !NewsArticle.STATUS_DRAFT.equalsIgnoreCase(requestedStatus)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Create a draft first; publication requires an Editor or Admin");
        }
        request.setNewsStatus(requestedStatus);

        List<Tag> tags = new ArrayList<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = tagRepository.findAllById(request.getTagIds());
        }

        NewsArticle article = newsArticleMapper.toEntity(request, category, createdBy, tags);
        if (NewsArticle.STATUS_PUBLISHED.equals(requestedStatus) && article.getPublishedDate() == null) {
            article.setPublishedDate(LocalDateTime.now(ZoneOffset.UTC));
        }
        NewsArticle saved = newsArticleRepository.save(article);

        if (auditLogService != null) {
            try {
                auditLogService.logAction(saved.getNewsArticleId(), createdBy.getAccountId(), createdBy.getAccountName(),
                        "CREATE", null, requestedStatus, "Tạo bài viết mới với trạng thái " + requestedStatus);
            } catch (Exception ignored) {
            }
        }

        return newsArticleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public NewsArticleResponseDto updateArticle(Integer id, NewsArticleRequestDto request) {
        NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News article not found with id: " + id));
        requireEditableDraft(article);

        boolean canPublish = SecurityUtils.isAdmin() || SecurityUtils.hasRole("EDITOR");
        String oldStatus = article.getNewsStatus();

        if (request.getNewsStatus() != null && !request.getNewsStatus().isBlank()) {
            String requestedStatus = request.getNewsStatus().trim().toUpperCase();
            if ("1".equals(requestedStatus) || "ACTIVE".equals(requestedStatus)) {
                requestedStatus = NewsArticle.STATUS_PUBLISHED;
            } else if ("0".equals(requestedStatus) || "INACTIVE".equals(requestedStatus)) {
                requestedStatus = NewsArticle.STATUS_DRAFT;
            }

            if (!canPublish && !requestedStatus.equalsIgnoreCase(article.getNewsStatus())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Use the Editor publication endpoint to change status");
            }
            request.setNewsStatus(requestedStatus);
            if (NewsArticle.STATUS_PUBLISHED.equals(requestedStatus) && article.getPublishedDate() == null) {
                article.setPublishedDate(LocalDateTime.now(ZoneOffset.UTC));
            }
        }

        if (request.getNewsTitle() != null && request.getNewsTitle().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "News title cannot be empty");
        }
        if (request.getNewsContent() != null && request.getNewsContent().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "News content cannot be empty");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found with id: " + request.getCategoryId()));
        }

        SystemAccount updatedBy = SecurityUtils.getCurrentUser().getAccount();

        List<Tag> tags = null;
        if (request.getTagIds() != null) {
            tags = request.getTagIds().isEmpty() ? new ArrayList<>() : tagRepository.findAllById(request.getTagIds());
        }

        newsArticleMapper.updateEntity(article, request, category, updatedBy, tags);
        NewsArticle updated = newsArticleRepository.save(article);

        if (auditLogService != null && request.getNewsStatus() != null && !request.getNewsStatus().equalsIgnoreCase(oldStatus)) {
            try {
                auditLogService.logAction(id, updatedBy.getAccountId(), updatedBy.getAccountName(),
                        "STATUS_CHANGE", oldStatus, article.getNewsStatus(), "Cập nhật bài viết & đổi trạng thái sang " + article.getNewsStatus());
            } catch (Exception ignored) {
            }
        }

        return newsArticleMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteArticle(Integer id) {
        NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News article not found with id: " + id));
        requireEditableDraft(article);
        newsTagRepository.deleteByArticleId(id);
        newsArticleRepository.delete(article);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public NewsArticleResponseDto updateArticleStatus(Integer id, String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "News status cannot be empty");
        }
        String normalizedStatus = status.trim().toUpperCase();
        if ("1".equals(normalizedStatus) || "ACTIVE".equals(normalizedStatus)) {
            normalizedStatus = NewsArticle.STATUS_PUBLISHED;
        } else if ("0".equals(normalizedStatus) || "INACTIVE".equals(normalizedStatus)) {
            normalizedStatus = NewsArticle.STATUS_ARCHIVED;
        }

        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid news status: " + status + ". Allowed: " + VALID_STATUSES);
        }

        NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News article not found with id: " + id));

        String oldStatus = article.getNewsStatus();
        article.setNewsStatus(normalizedStatus);
        if (NewsArticle.STATUS_PUBLISHED.equals(normalizedStatus) && article.getPublishedDate() == null) {
            article.setPublishedDate(LocalDateTime.now(ZoneOffset.UTC));
        }
        SystemAccount currentUser = SecurityUtils.getCurrentUser().getAccount();
        article.setUpdatedBy(currentUser);
        article.setModifiedDate(LocalDateTime.now(ZoneOffset.UTC));
        NewsArticle saved = newsArticleRepository.save(article);

        if (auditLogService != null) {
            try {
                auditLogService.logAction(id, currentUser.getAccountId(), currentUser.getAccountName(),
                        "STATUS_CHANGE", oldStatus, normalizedStatus, "Đổi trạng thái bài viết từ " + oldStatus + " sang " + normalizedStatus);
            } catch (Exception ignored) {
            }
        }

        return newsArticleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public List<NewsArticleResponseDto> publishedArticles(String keyword, Integer categoryId, Integer tagId) {
        return newsArticleRepository.searchArticles(keyword, categoryId, NewsArticle.STATUS_PUBLISHED, tagId).stream()
                .map(newsArticleMapper::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public NewsArticleResponseDto publishedArticle(Integer id) {
        NewsArticle article = newsArticleRepository.findById(id)
                .filter(value -> NewsArticle.STATUS_PUBLISHED.equalsIgnoreCase(value.getNewsStatus()) || "1".equals(value.getNewsStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Published article not found"));
        return newsArticleMapper.toResponseDto(article);
    }

    private boolean canRead(NewsArticle article) {
        return SecurityUtils.isAdmin() || SecurityUtils.hasRole("EDITOR")
                || (article.getCreatedBy() != null && java.util.Objects.equals(article.getCreatedBy().getAccountId(), SecurityUtils.getCurrentAccountId()));
    }

    private void requireEditableDraft(NewsArticle article) {
        SecurityUtils.requireArticleOwnerOrAdmin(article);
        if (!SecurityUtils.isAdmin()
                && !NewsArticle.STATUS_DRAFT.equalsIgnoreCase(article.getNewsStatus())
                && !NewsArticle.STATUS_CHANGES_REQUESTED.equalsIgnoreCase(article.getNewsStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only your own draft or changes-requested article can be changed");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsArticleResponseDto> getArticlesByCategory(Integer categoryId) {
        return newsArticleRepository.findByCategory_CategoryId(categoryId).stream()
                .filter(this::canRead).map(newsArticleMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsArticleResponseDto> getArticlesByTag(Integer tagId) {
        return newsArticleRepository.findByTagId(tagId).stream()
                .filter(this::canRead).map(newsArticleMapper::toResponseDto)
                .toList();
    }
}
