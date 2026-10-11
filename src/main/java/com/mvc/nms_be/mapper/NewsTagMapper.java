package com.mvc.nms_be.mapper;

import com.mvc.nms_be.dto.request.NewsTagRequestDto;
import com.mvc.nms_be.dto.response.NewsTagResponseDto;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.entity.NewsTag;
import com.mvc.nms_be.entity.NewsTagId;
import com.mvc.nms_be.entity.Tag;
import org.springframework.stereotype.Component;

@Component
public class NewsTagMapper {

    // The service loads these entities using the two IDs from the request.
    public NewsTag toEntity(NewsTagRequestDto request, NewsArticle article, Tag tag) {
        NewsTag newsTag = new NewsTag();
        newsTag.setId(new NewsTagId(request.getNewsArticleId(), request.getTagId()));
        newsTag.setNewsArticle(article);
        newsTag.setTag(tag);
        return newsTag;
    }

    public NewsTagResponseDto toResponseDto(NewsTag newsTag) {
        return new NewsTagResponseDto(
                newsTag.getNewsArticle().getNewsArticleId(),
                newsTag.getTag().getTagId()
        );
    }
}
