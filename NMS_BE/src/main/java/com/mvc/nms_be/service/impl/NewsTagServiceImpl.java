package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.NewsTagRequestDto;
import com.mvc.nms_be.dto.response.NewsTagResponseDto;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.entity.NewsTag;
import com.mvc.nms_be.entity.NewsTagId;
import com.mvc.nms_be.entity.Tag;
import com.mvc.nms_be.mapper.NewsTagMapper;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.repository.NewsTagRepository;
import com.mvc.nms_be.repository.TagRepository;
import com.mvc.nms_be.service.NewsTagService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NewsTagServiceImpl implements NewsTagService {

    private final NewsTagRepository newsTagRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final TagRepository tagRepository;
    private final NewsTagMapper newsTagMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NewsTagResponseDto> getAllNewsTags() {
        return newsTagRepository.findAll().stream()
                .map(newsTagMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsTagResponseDto> getTagsByArticleId(Integer articleId) {
        return newsTagRepository.findByNewsArticle_NewsArticleId(articleId).stream()
                .map(newsTagMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsTagResponseDto> getArticlesByTagId(Integer tagId) {
        return newsTagRepository.findByTag_TagId(tagId).stream()
                .map(newsTagMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public NewsTagResponseDto addTagToArticle(NewsTagRequestDto request) {
        if (request.getNewsArticleId() == null || request.getTagId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both newsArticleId and tagId are required");
        }

        if (newsTagRepository.existsByNewsArticle_NewsArticleIdAndTag_TagId(request.getNewsArticleId(), request.getTagId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This tag is already assigned to this news article");
        }

        NewsArticle article = newsArticleRepository.findById(request.getNewsArticleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News article not found with id: " + request.getNewsArticleId()));

        Tag tag = tagRepository.findById(request.getTagId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found with id: " + request.getTagId()));

        NewsTag newsTag = newsTagMapper.toEntity(request, article, tag);
        NewsTag saved = newsTagRepository.save(newsTag);
        return newsTagMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public void removeTagFromArticle(Integer articleId, Integer tagId) {
        NewsTagId id = new NewsTagId(articleId, tagId);
        if (!newsTagRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag association not found for article " + articleId + " and tag " + tagId);
        }
        newsTagRepository.deleteById(id);
    }
}
