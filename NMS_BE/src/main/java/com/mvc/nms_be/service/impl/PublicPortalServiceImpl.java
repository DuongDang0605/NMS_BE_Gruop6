package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.response.PublicArticleDto;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.exception.ResourceNotFoundException;
import com.mvc.nms_be.repository.ArticleReactionRepository;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.repository.NewsTagRepository;
import com.mvc.nms_be.service.PublicPortalService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mvc.nms_be.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicPortalServiceImpl implements PublicPortalService {

    private final NewsArticleRepository newsArticleRepository;
    private final ArticleReactionRepository reactionRepository;
    private final NewsTagRepository newsTagRepository;

    @Override
    public Map<String, Object> getPublishedArticles(String keyword, Integer categoryId,
                                                     Integer tagId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0, size between 1 and 100");
        }
        int offset = page * size;

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        List<Object[]> rows = newsArticleRepository.findPublishedArticles(kw, categoryId, tagId, offset, size);
        long total = newsArticleRepository.countPublishedArticles(kw, categoryId, tagId);

        List<Map<String, Object>> articles = rows.stream().map(r -> Map.<String, Object>of(
                "articleId",    ((Number) r[0]).intValue(),
                "title",        (String) r[1],
                "headline",     r[2] != null ? (String) r[2] : "",
                "createdDate",  r[3].toString(),
                "source",       r[4] != null ? (String) r[4] : "",
                "viewCount",    ((Number) r[5]).intValue(),
                "likeCount",    ((Number) r[6]).intValue(),
                "categoryName", (String) r[7],
                "authorName",   (String) r[8]
        )).toList();

        return Map.of(
                "content",       articles,
                "page",          page,
                "size",          size,
                "totalElements", total,
                "totalPages",    (int) Math.ceil((double) total / size));
    }

    @Override
    public PublicArticleDto getArticleDetail(Integer articleId) {
        NewsArticle article = newsArticleRepository.findById(articleId)
                .filter(a -> a.getNewsStatus() == 1)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + articleId));

        // Kiểm tra user đăng nhập hay chưa để trả cờ isLikedByCurrentUser
        Boolean isLiked = false;
        Integer currentUserId = getCurrentUserIdOrNull();
        if (currentUserId != null) {
            isLiked = reactionRepository.existsByNewsArticleIdAndAccount_AccountId(articleId, currentUserId);
        }

        // Lấy danh sách tên tag của bài viết
        List<String> tags = newsTagRepository.findByNewsArticle_NewsArticleId(articleId)
                .stream()
                .map(nt -> nt.getTag().getTagName())
                .toList();

        return new PublicArticleDto(
                article.getNewsArticleId(),
                article.getNewsTitle(),
                article.getHeadline(),
                article.getNewsContent(),
                article.getNewsSource(),
                article.getCategory() != null ? article.getCategory().getCategoryName() : null,
                article.getCreatedBy() != null ? article.getCreatedBy().getAccountName() : null,
                article.getCreatedDate(),
                article.getViewCount(),
                article.getLikeCount(),
                isLiked,
                tags);
    }

    /** Trả về accountId của user đang đăng nhập, null nếu chưa đăng nhập. */
    private Integer getCurrentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getAccount().getAccountId();
        }
        return null;
    }
}
