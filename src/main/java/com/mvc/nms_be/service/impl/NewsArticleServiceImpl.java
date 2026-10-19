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
import com.mvc.nms_be.repository.SystemAccountRepository;
import com.mvc.nms_be.repository.TagRepository;
import com.mvc.nms_be.service.NewsArticleService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NewsArticleServiceImpl implements NewsArticleService {

    private final NewsArticleRepository newsArticleRepository;
    private final CategoryRepository categoryRepository;
    private final SystemAccountRepository systemAccountRepository;
    private final TagRepository tagRepository;
    private final NewsTagRepository newsTagRepository;
    private final NewsArticleMapper newsArticleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NewsArticleResponseDto> getAllArticles() {
        return newsArticleRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDate")).stream()
                .map(newsArticleMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsArticleResponseDto> searchArticles(String keyword, Integer categoryId, Integer newsStatus, Integer tagId) {
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return newsArticleRepository.searchArticles(trimmedKeyword, categoryId, newsStatus, tagId).stream()
                .map(newsArticleMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NewsArticleResponseDto getArticleById(Integer id) {
        NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News article not found with id: " + id));
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

        SystemAccount createdBy;
        if (request.getCreatedById() != null) {
            createdBy = systemAccountRepository.findById(request.getCreatedById())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Creator account not found with id: " + request.getCreatedById()));
        } else {
            // Default to first staff or admin account if createdById was not explicitly supplied
            createdBy = systemAccountRepository.findAll().stream()
                    .filter(acc -> acc.getAccountRole() != null && (acc.getAccountRole() == 1 || acc.getAccountRole() == 2))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "createdById is required and no default Staff/Admin account was found"));
        }

        // Validate creator role (1 = Admin, 2 = Staff)
        if (createdBy.getAccountRole() != null && createdBy.getAccountRole() != 1 && createdBy.getAccountRole() != 2) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Admin (1) or Staff (2) can create news articles");
        }

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

        SystemAccount updatedBy = null;
        if (request.getUpdatedById() != null) {
            updatedBy = systemAccountRepository.findById(request.getUpdatedById())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Updater account not found with id: " + request.getUpdatedById()));
            if (updatedBy.getAccountRole() != null && updatedBy.getAccountRole() != 1 && updatedBy.getAccountRole() != 2) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Admin (1) or Staff (2) can update news articles");
            }
        }

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
        newsTagRepository.deleteByArticleId(id);
        newsArticleRepository.delete(article);
    }

    @Override
    @Transactional
    public NewsArticleResponseDto updateArticleStatus(Integer id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "News status must be 0 (inactive) or 1 (active)");
        }
        NewsArticle article = newsArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News article not found with id: " + id));
        article.setNewsStatus(status);
        NewsArticle saved = newsArticleRepository.save(article);
        return newsArticleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsArticleResponseDto> getArticlesByCategory(Integer categoryId) {
        return newsArticleRepository.findByCategory_CategoryId(categoryId).stream()
                .map(newsArticleMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsArticleResponseDto> getArticlesByTag(Integer tagId) {
        return newsArticleRepository.findByTagId(tagId).stream()
                .map(newsArticleMapper::toResponseDto)
                .toList();
    }
}
