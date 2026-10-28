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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page >= 0, size between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<NewsArticle> articlePage = newsArticleRepository.findPublishedArticles(kw, categoryId, tagId, pageable);

        List<Map<String, Object>> articles = articlePage.getContent().stream().map(a -> Map.<String, Object>of(
                "articleId",    a.getNewsArticleId(),
                "title",        a.getNewsTitle(),
                "headline",     a.getHeadline() != null ? a.getHeadline() : "",
                "createdDate",  a.getCreatedDate() != null ? a.getCreatedDate().toString() : "",
                "source",       a.getNewsSource() != null ? a.getNewsSource() : "",
                "viewCount",    a.getViewCount(),
                "likeCount",    a.getLikeCount(),
                "categoryName", a.getCategory() != null ? a.getCategory().getCategoryName() : "",
                "authorName",   a.getCreatedBy() != null ? a.getCreatedBy().getAccountName() : ""
        )).toList();

        return Map.of(
                "content",       articles,
                "page",          articlePage.getNumber(),
                "size",          articlePage.getSize(),
                "totalElements", articlePage.getTotalElements(),
                "totalPages",    articlePage.getTotalPages());
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

        // 1 query trực tiếp lấy tên tag, tránh triệt để N+1 lazy loading
        List<String> tags = newsTagRepository.findTagNamesByArticleId(articleId);

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
