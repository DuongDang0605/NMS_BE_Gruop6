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
import com.mvc.nms_be.service.NewsArticleService;
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

    private final NewsArticleRepository newsArticleRepository;
    private final CategoryRepository categoryRepository;

    private final TagRepository tagRepository;
    private final NewsTagRepository newsTagRepository;
    private final NewsArticleMapper newsArticleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NewsArticleResponseDto> getAllArticles() {
        return newsArticleRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDate")).stream()
                .filter(this::canRead).map(newsArticleMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsArticleResponseDto> searchArticles(String keyword, Integer categoryId, Integer newsStatus, Integer tagId) {
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return newsArticleRepository.searchArticles(trimmedKeyword, categoryId, newsStatus, tagId).stream()
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
        if (!SecurityUtils.isAdmin() && request.getNewsStatus() != null && request.getNewsStatus() != 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Create a draft first; publication requires an Editor or Admin");
        }
        if (request.getNewsStatus() == null) request.setNewsStatus(0);
        List<Tag> tags = new ArrayList<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = tagRepository.findAllById(request.getTagIds());
        }

        NewsArticle article = newsArticleMapper.toEntity(request, category, createdBy, tags);
        NewsArticle saved = newsArticleRepository.save(article);
        return newsArticleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public NewsArticleResponseDto updateArticle(Integer id, NewsArticleRequestDto request) {
        NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News article not found with id: " + id));
        requireEditableDraft(article);
        if (!SecurityUtils.isAdmin() && request.getNewsStatus() != null
                && !request.getNewsStatus().equals(article.getNewsStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Use the Editor publication endpoint to change status");
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
    public NewsArticleResponseDto updateArticleStatus(Integer id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "News status must be 0 (inactive) or 1 (active)");
        }
        NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News article not found with id: " + id));
        article.setNewsStatus(status);
        article.setUpdatedBy(SecurityUtils.getCurrentUser().getAccount());
        NewsArticle saved = newsArticleRepository.save(article);
        return newsArticleMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public List<NewsArticleResponseDto> publishedArticles(String keyword, Integer categoryId, Integer tagId) {
        return newsArticleRepository.searchArticles(keyword, categoryId, 1, tagId).stream()
                .map(newsArticleMapper::toResponseDto).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public NewsArticleResponseDto publishedArticle(Integer id) {
        NewsArticle article = newsArticleRepository.findById(id)
                .filter(value -> Integer.valueOf(1).equals(value.getNewsStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Published article not found"));
        return newsArticleMapper.toResponseDto(article);
    }

    private boolean canRead(NewsArticle article) {
        return SecurityUtils.isAdmin() || SecurityUtils.hasRole("EDITOR")
                || java.util.Objects.equals(article.getCreatedBy().getAccountId(), SecurityUtils.getCurrentAccountId());
    }

    private void requireEditableDraft(NewsArticle article) {
        SecurityUtils.requireArticleOwnerOrAdmin(article);
        if (!SecurityUtils.isAdmin() && !Integer.valueOf(0).equals(article.getNewsStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only your own draft can be changed");
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
