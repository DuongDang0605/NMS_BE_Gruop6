package com.mvc.nms_be.mapper;

import com.mvc.nms_be.dto.request.NewsArticleRequestDto;
import com.mvc.nms_be.dto.response.NewsArticleResponseDto;
import com.mvc.nms_be.entity.Category;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.entity.NewsTag;
import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.entity.Tag;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NewsArticleMapper {

    // The service resolves categoryId/tagIds and supplies the authenticated creator.
    public NewsArticle toEntity(NewsArticleRequestDto request, Category category,
                                SystemAccount createdBy, List<Tag> tags) {
        NewsArticle article = new NewsArticle();
        article.setNewsTitle(request.getNewsTitle());
        article.setHeadline(request.getHeadline());
        article.setNewsContent(request.getNewsContent());
        article.setNewsSource(request.getNewsSource());
        article.setCategory(category);
        article.setNewsStatus(request.getNewsStatus());
        article.setCreatedBy(createdBy);

        for (Tag tag : tags) {
            article.getNewsTags().add(new NewsTag(article, tag));
        }
        return article;
    }

    public NewsArticleResponseDto toResponseDto(NewsArticle article) {
        List<Integer> tagIds = article.getNewsTags().stream()
                .map(newsTag -> newsTag.getTag().getTagId())
                .toList();

        return new NewsArticleResponseDto(
                article.getNewsArticleId(),
                article.getNewsTitle(),
                article.getHeadline(),
                article.getCreatedDate(),
                article.getNewsContent(),
                article.getNewsSource(),
                article.getCategory().getCategoryId(),
                article.getNewsStatus(),
                article.getCreatedBy().getAccountId(),
                article.getUpdatedBy() == null ? null : article.getUpdatedBy().getAccountId(),
                article.getModifiedDate(),
                tagIds
        );
    }
}
