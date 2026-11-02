package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.NewsTagRequestDto;
import com.mvc.nms_be.dto.response.NewsTagResponseDto;
import java.util.List;

public interface NewsTagService {

    List<NewsTagResponseDto> getAllNewsTags();

    List<NewsTagResponseDto> getTagsByArticleId(Integer articleId);

    List<NewsTagResponseDto> getArticlesByTagId(Integer tagId);

    NewsTagResponseDto addTagToArticle(NewsTagRequestDto request);

    void removeTagFromArticle(Integer articleId, Integer tagId);
}
