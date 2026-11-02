package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.response.TrendingArticleDto;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.repository.CategoryRepository;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.repository.NotificationRepository;
import com.mvc.nms_be.repository.SystemAccountRepository;
import com.mvc.nms_be.repository.TagRepository;
import com.mvc.nms_be.service.DashboardService;
import com.mvc.nms_be.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN', 'CONTRIBUTOR', 'EDITOR', 'MODERATOR')")
public class DashboardServiceImpl implements DashboardService {

    private final NewsArticleRepository articleRepository;
    private final NotificationRepository notificationRepository;
    private final SystemAccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    @Override
    public Map<String, Object> overview() {
        if (SecurityUtils.hasRole("CONTRIBUTOR")) {
            var articles = articleRepository.findByCreatedBy_AccountId(SecurityUtils.getCurrentAccountId());
            long active = articles.stream().filter(article -> NewsArticle.STATUS_PUBLISHED.equalsIgnoreCase(article.getNewsStatus()) || "1".equals(article.getNewsStatus())).count();
            return Map.of("scope", "personal", "totalArticles", articles.size(),
                    "activeArticles", active, "inactiveArticles", articles.size() - active);
        }
        if (SecurityUtils.hasRole("MODERATOR")) {
            var result = new HashMap<String, Object>(notifications(SecurityUtils.getCurrentAccountId()));
            result.put("scope", "moderation");
            return result;
        }
        long totalArticles = articleRepository.count();
        long activeArticles = articleRepository.countByNewsStatus(NewsArticle.STATUS_PUBLISHED);
        if (SecurityUtils.hasRole("EDITOR")) {
            return Map.of("scope", "editorial", "totalArticles", totalArticles, "activeArticles", activeArticles,
                    "inactiveArticles", totalArticles - activeArticles, "totalCategories", categoryRepository.count(),
                    "totalTags", tagRepository.count());
        }
        return Map.of(
                "totalAccounts", accountRepository.count(),
                "totalAdmins", accountRepository.countByAccountRole(SystemAccount.ADMIN_ROLE),
                "totalContributors", accountRepository.countByAccountRole(SystemAccount.CONTRIBUTOR_ROLE),
                "totalCategories", categoryRepository.count(),
                "totalTags", tagRepository.count(),
                "totalArticles", totalArticles,
                "activeArticles", activeArticles,
                "inactiveArticles", totalArticles - activeArticles,
                "totalNotifications", notificationRepository.count());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public Map<String, Object> news() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<Map<String, Object>> byCategory = new ArrayList<>();
        for (Object[] row : articleRepository.countArticlesByCategory()) {
            byCategory.add(Map.of(
                    "categoryId", row[0],
                    "categoryName", row[1],
                    "articleCount", row[2]));
        }

        // 1 query cho cả 30 ngày: tổng 30 ngày, tổng 7 ngày (rolling) và số bài theo từng ngày UTC
        long last30Days = 0;
        long last7Days = 0;
        Map<String, Long> countByDay = new HashMap<>();
        for (Object[] row : articleRepository.countArticlesPerDaySince(now.minusDays(30), now.minusDays(7))) {
            long dayCount = toLong(row[1]);
            last30Days += dayCount;
            last7Days += toLong(row[2]);
            countByDay.put(row[0].toString(), dayCount);
        }

        List<Map<String, Object>> daily = new ArrayList<>();
        LocalDate today = now.toLocalDate();
        for (int daysAgo = 6; daysAgo >= 0; daysAgo--) {
            String date = today.minusDays(daysAgo).toString();
            daily.add(Map.of("date", date, "count", countByDay.getOrDefault(date, 0L)));
        }

        return Map.of(
                "articlesLast7Days", last7Days,
                "articlesLast30Days", last30Days,
                "articlesByCategory", byCategory,
                "dailyArticlesLast7Days", daily);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTRIBUTOR', 'EDITOR', 'MODERATOR') and @recipientAuthorization.canAccess(#recipientId)")
    public Map<String, Object> notifications(Integer recipientId) {
        if (recipientId == null || recipientId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipientId must be a positive integer");
        }
        if (!accountRepository.existsById(recipientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found");
        }
        long total = notificationRepository.countByRecipient_AccountId(recipientId);
        long unread = notificationRepository.countByRecipient_AccountIdAndIsRead(recipientId, 0);
        long read = total - unread;
        return Map.of(
                "totalNotifications", total,
                "readNotifications", read,
                "unreadNotifications", unread);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTRIBUTOR', 'EDITOR')")
    public Map<String, Object> engagement() {
        if (SecurityUtils.hasRole("CONTRIBUTOR")) {
            var ownArticles = articleRepository.findByCreatedBy_AccountId(SecurityUtils.getCurrentAccountId());
            List<TrendingArticleDto> trending = ownArticles.stream()
                    .filter(article -> NewsArticle.STATUS_PUBLISHED.equalsIgnoreCase(article.getNewsStatus()) || "1".equals(article.getNewsStatus()))
                    .sorted(java.util.Comparator.comparingInt(com.mvc.nms_be.entity.NewsArticle::getViewCount).reversed()
                            .thenComparing(java.util.Comparator.comparingInt(com.mvc.nms_be.entity.NewsArticle::getLikeCount).reversed()))
                    .limit(5).map(article -> new TrendingArticleDto(article.getNewsArticleId(), article.getNewsTitle(),
                            article.getViewCount(), article.getLikeCount())).toList();
            return Map.of("scope", "personal", "totalViews", ownArticles.stream().mapToLong(article -> article.getViewCount()).sum(),
                    "totalLikes", ownArticles.stream().mapToLong(article -> article.getLikeCount()).sum(), "top5Trending", trending);
        }
        // tổng view và like toàn hệ thống
        List<Object[]> sumRows = articleRepository.sumViewsAndLikes();
        long totalViews = 0;
        long totalLikes = 0;
        if (sumRows != null && !sumRows.isEmpty()) {
            Object[] sums = sumRows.get(0);
            totalViews = toLong(sums[0]);
            totalLikes = toLong(sums[1]);
        }

        // top 5 bài trending theo viewCount - type-safe từ Spring Data JPA
        List<TrendingArticleDto> trending = articleRepository
                .findTop5ByNewsStatusOrderByViewCountDescLikeCountDesc(NewsArticle.STATUS_PUBLISHED)
                .stream()
                .map(a -> new TrendingArticleDto(
                        a.getNewsArticleId(),
                        a.getNewsTitle(),
                        a.getViewCount(),
                        a.getLikeCount()))
                .toList();

        return Map.of(
                "totalViews", totalViews,
                "totalLikes", totalLikes,
                "top5Trending", trending);
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof Object[] arr && arr.length > 0) {
            return toLong(arr[0]);
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return 0L;
        }
    }
}
