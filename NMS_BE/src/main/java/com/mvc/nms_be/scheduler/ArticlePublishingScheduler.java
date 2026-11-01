package com.mvc.nms_be.scheduler;

import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.service.ArticleAuditLogService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticlePublishingScheduler {

    private final NewsArticleRepository newsArticleRepository;
    private final ArticleAuditLogService auditLogService;

    // Chạy mỗi 60 giây quét 1 lần
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void publishScheduledArticles() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<NewsArticle> readyArticles = newsArticleRepository
                .findAllByNewsStatusAndScheduledPublishDateLessThanEqual(NewsArticle.STATUS_SCHEDULED, now);

        if (readyArticles.isEmpty()) {
            return;
        }

        log.info("Phát hiện {} bài viết đến thời điểm xuất bản tự động", readyArticles.size());

        for (NewsArticle article : readyArticles) {
            article.setNewsStatus(NewsArticle.STATUS_PUBLISHED);
            article.setPublishedDate(now);
            article.setModifiedDate(now);
            newsArticleRepository.save(article);

            auditLogService.logAction(
                    article.getNewsArticleId(),
                    0,
                    "SYSTEM_SCHEDULER",
                    "AUTO_PUBLISH",
                    NewsArticle.STATUS_SCHEDULED,
                    NewsArticle.STATUS_PUBLISHED,
                    "Hệ thống tự động kích hoạt xuất bản bài viết theo lịch hẹn"
            );
            log.info("Đã xuất bản tự động bài viết ID: {}", article.getNewsArticleId());
        }
    }
}

