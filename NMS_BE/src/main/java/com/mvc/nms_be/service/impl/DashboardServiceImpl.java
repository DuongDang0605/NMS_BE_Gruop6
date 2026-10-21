package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.repository.CategoryRepository;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.repository.NotificationRepository;
import com.mvc.nms_be.repository.SystemAccountRepository;
import com.mvc.nms_be.repository.TagRepository;
import com.mvc.nms_be.service.DashboardService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

    private final SystemAccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final NewsArticleRepository articleRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public Map<String, Object> overview() {
        return Map.of(
                "totalAccounts", accountRepository.count(),
                "totalAdmins", accountRepository.countByAccountRole(SystemAccount.ADMIN_ROLE),
                "totalStaff", accountRepository.countByAccountRole(SystemAccount.STAFF_ROLE),
                "totalCategories", categoryRepository.count(),
                "totalTags", tagRepository.count(),
                "totalArticles", articleRepository.count(),
                "activeArticles", articleRepository.countByNewsStatus(1),
                "inactiveArticles", articleRepository.countByNewsStatus(0),
                "totalNotifications", notificationRepository.count());
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

        List<Map<String, Object>> daily = new ArrayList<>();
        LocalDate today = now.toLocalDate();
        for (int daysAgo = 6; daysAgo >= 0; daysAgo--) {
            LocalDate date = today.minusDays(daysAgo);
            long count = articleRepository.countByCreatedDateGreaterThanEqualAndCreatedDateLessThan(
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay());
            daily.add(Map.of("date", date.toString(), "count", count));
        }

        return Map.of(
                "articlesLast7Days", articleRepository.countByCreatedDateGreaterThanEqual(now.minusDays(7)),
                "articlesLast30Days", articleRepository.countByCreatedDateGreaterThanEqual(now.minusDays(30)),
                "articlesByCategory", byCategory,
                "dailyArticlesLast7Days", daily);
    }

    @Override
    public Map<String, Object> notifications(Integer recipientId) {
        if (recipientId == null || recipientId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipientId must be a positive integer");
        }
        if (!accountRepository.existsById(recipientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found");
        }
        return Map.of(
                "totalNotifications", notificationRepository.countByRecipient_AccountId(recipientId),
                "readNotifications", notificationRepository.countByRecipient_AccountIdAndIsRead(recipientId, 1),
                "unreadNotifications", notificationRepository.countByRecipient_AccountIdAndIsRead(recipientId, 0));
    }
}
