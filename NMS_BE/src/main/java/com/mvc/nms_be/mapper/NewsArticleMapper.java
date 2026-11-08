package com.mvc.nms_be.mapper;

import com.mvc.nms_be.dto.request.NewsArticleRequestDto;
import com.mvc.nms_be.dto.response.NewsArticleResponseDto;
import com.mvc.nms_be.dto.response.TagResponseDto;
import com.mvc.nms_be.entity.Category;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.entity.NewsTag;
import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.entity.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class NewsArticleMapper {

    public NewsArticle toEntity(NewsArticleRequestDto request, Category category,
                                SystemAccount createdBy, List<Tag> tags) {
        NewsArticle article = new NewsArticle();
        article.setNewsTitle(request.getNewsTitle());
        article.setHeadline(request.getHeadline());
        article.setNewsContent(request.getNewsContent());
        article.setNewsSource(request.getNewsSource());
        article.setCategory(category);
        article.setNewsStatus(request.getNewsStatus() != null && !request.getNewsStatus().isBlank()
                ? request.getNewsStatus() : NewsArticle.STATUS_DRAFT);
        article.setScheduledPublishDate(request.getScheduledPublishDate());
        if (request.getAllowComments() != null) {
            article.setAllowComments(request.getAllowComments());
        }
        article.setCreatedBy(createdBy);

        if (tags != null) {
            for (Tag tag : tags) {
                article.getNewsTags().add(new NewsTag(article, tag));
            }
        }
        return article;
    }

    public void updateEntity(NewsArticle article, NewsArticleRequestDto request,
                             Category category, SystemAccount updatedBy, List<Tag> tags) {
        if (request.getNewsTitle() != null) {
            article.setNewsTitle(request.getNewsTitle());
        }
        if (request.getHeadline() != null) {
            article.setHeadline(request.getHeadline());
        }
        if (request.getNewsContent() != null) {
            article.setNewsContent(request.getNewsContent());
        }
        if (request.getNewsSource() != null) {
            article.setNewsSource(request.getNewsSource());
        }
        if (category != null) {
            article.setCategory(category);
        }
        if (request.getAllowComments() != null) {
            article.setAllowComments(request.getAllowComments());
        }
        if (updatedBy != null) {
            article.setUpdatedBy(updatedBy);
        }

        if (tags != null) {
            Set<Integer> newTagIds = tags.stream()
                    .map(Tag::getTagId)
                    .collect(Collectors.toSet());

            // 1. Xóa các thẻ không còn được chọn
            article.getNewsTags().removeIf(nt -> !newTagIds.contains(nt.getTag().getTagId()));

            // 2. Tìm danh sách ID thẻ đã có sẵn
            Set<Integer> existingTagIds = article.getNewsTags().stream()
                    .map(nt -> nt.getTag().getTagId())
                    .collect(Collectors.toSet());

            // 3. Chỉ thêm các thẻ mới chưa tồn tại để tránh vi phạm Primary Key constraint
            for (Tag tag : tags) {
                if (!existingTagIds.contains(tag.getTagId())) {
                    article.getNewsTags().add(new NewsTag(article, tag));
                }
            }
        }
    }

    public NewsArticleResponseDto toResponseDto(NewsArticle article) {
        List<Integer> tagIds = article.getNewsTags() == null ? new ArrayList<>() :
                article.getNewsTags().stream()
                        .map(newsTag -> newsTag.getTag().getTagId())
                        .toList();

        List<TagResponseDto> tags = article.getNewsTags() == null ? new ArrayList<>() :
                article.getNewsTags().stream()
                        .map(newsTag -> new TagResponseDto(
                                newsTag.getTag().getTagId(),
                                newsTag.getTag().getTagName(),
                                newsTag.getTag().getNote()))
                        .toList();

        return new NewsArticleResponseDto(
                article.getNewsArticleId(),
                article.getNewsTitle(),
                article.getHeadline(),
                article.getCreatedDate(),
                article.getNewsContent(),
                article.getNewsSource(),
                article.getCategory() != null ? article.getCategory().getCategoryId() : null,
                article.getCategory() != null ? article.getCategory().getCategoryName() : null,
                article.getNewsStatus(),
                article.getScheduledPublishDate(),
                article.getPublishedDate(),
                article.getViewCount() != null ? article.getViewCount() : 0,
                article.getLikeCount() != null ? article.getLikeCount() : 0,
                article.getCommentCount() != null ? article.getCommentCount() : 0,
                article.getAllowComments() != null ? article.getAllowComments() : true,
                article.getCreatedBy() != null ? article.getCreatedBy().getAccountId() : null,
                article.getCreatedBy() != null ? article.getCreatedBy().getAccountName() : null,
                article.getUpdatedBy() != null ? article.getUpdatedBy().getAccountId() : null,
                article.getUpdatedBy() != null ? article.getUpdatedBy().getAccountName() : null,
                article.getModifiedDate(),
                tagIds,
                tags
        );
    }
}
