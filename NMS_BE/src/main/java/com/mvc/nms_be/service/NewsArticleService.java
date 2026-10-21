package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.NewsArticleRequestDto;
import com.mvc.nms_be.dto.response.NewsArticleResponseDto;
import java.util.List;

public interface NewsArticleService {

    List<NewsArticleResponseDto> getAllArticles();

    List<NewsArticleResponseDto> searchArticles(String keyword, Integer categoryId, Integer newsStatus, Integer tagId);

    NewsArticleResponseDto getArticleById(Integer id);

    NewsArticleResponseDto createArticle(NewsArticleRequestDto request);

    NewsArticleResponseDto updateArticle(Integer id, NewsArticleRequestDto request);

    void deleteArticle(Integer id);

    NewsArticleResponseDto updateArticleStatus(Integer id, Integer status);

    List<NewsArticleResponseDto> getArticlesByCategory(Integer categoryId);

    List<NewsArticleResponseDto> getArticlesByTag(Integer tagId);
}
