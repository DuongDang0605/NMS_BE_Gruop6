package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.response.TrendingArticleDto;
import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.repository.NotificationRepository;
import com.mvc.nms_be.service.DashboardService;
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
public class DashboardServiceImpl implements DashboardService {

    private final NewsArticleRepository articleRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public Map<String, Object> overview() {
        Object[] row = articleRepository.dashboardOverview(
                SystemAccount.ADMIN_ROLE, SystemAccount.STAFF_ROLE).get(0);
        return Map.of(
                "totalAccounts", toLong(row[0]),
                "totalAdmins", toLong(row[1]),
                "totalStaff", toLong(row[2]),
                "totalCategories", toLong(row[3]),
                "totalTags", toLong(row[4]),
                "totalArticles", toLong(row[5]),
                "activeArticles", toLong(row[6]),
                "inactiveArticles", toLong(row[7]),
                "totalNotifications", toLong(row[8]));
    }

    @Override
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
    public Map<String, Object> notifications(Integer recipientId) {
        if (recipientId == null || recipientId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipientId must be a positive integer");
        }
        List<Object[]> rows = notificationRepository.countStatsForRecipient(recipientId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found");
        }
        Object[] row = rows.get(0);
        return Map.of(
                "totalNotifications", toLong(row[0]),
                "readNotifications", toLong(row[1]),
                "unreadNotifications", toLong(row[2]));
    }

    @Override
    public Map<String, Object> engagement() {
        // tổng view và like toàn hệ thống
        Object[] sums = articleRepository.sumViewsAndLikes();
        long totalViews = toLong(sums[0]);
        long totalLikes = toLong(sums[1]);

        // top 5 bài trending theo viewCount
        List<TrendingArticleDto> trending = new ArrayList<>();
        for (Object[] row : articleRepository.findTop5Trending()) {
            trending.add(new TrendingArticleDto(
                    toInt(row[0]),
                    (String) row[1],
                    toInt(row[2]),
                    toInt(row[3])));
        }

        return Map.of(
                "totalViews", totalViews,
                "totalLikes", totalLikes,
                "top5Trending", trending);
    }

    private static long toLong(Object value) {
        return ((Number) value).longValue();
    }

    private static int toInt(Object value) {
        return ((Number) value).intValue();
    }
}
