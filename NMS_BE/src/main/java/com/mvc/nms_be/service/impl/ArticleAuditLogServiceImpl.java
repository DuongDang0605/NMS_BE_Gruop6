package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.response.ArticleAuditLogResponseDto;
import com.mvc.nms_be.entity.ArticleAuditLog;
import com.mvc.nms_be.repository.ArticleAuditLogRepository;
import com.mvc.nms_be.service.ArticleAuditLogService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleAuditLogServiceImpl implements ArticleAuditLogService {

    private final ArticleAuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void logAction(Integer articleId, Integer actorId, String actorName,
                          String action, String fromStatus, String toStatus, String notes) {
        try {
            ArticleAuditLog logEntry = new ArticleAuditLog();
            logEntry.setNewsArticleId(articleId);
            logEntry.setActorId(actorId != null ? actorId : 0);
            logEntry.setActorName(actorName != null ? actorName : "UNKNOWN");
            logEntry.setAction(action);
            logEntry.setFromStatus(fromStatus);
            logEntry.setToStatus(toStatus);
            logEntry.setNotes(notes);
            logEntry.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

            auditLogRepository.save(logEntry);
            log.info("AuditLog recorded: articleId={}, action={}, {} -> {}", articleId, action, fromStatus, toStatus);
        } catch (Exception ex) {
            log.error("Failed to record audit log for articleId={}: {}", articleId, ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleAuditLogResponseDto> getAuditTrail(Integer articleId) {
        return auditLogRepository.findByNewsArticleIdOrderByCreatedAtDesc(articleId).stream()
                .map(log -> new ArticleAuditLogResponseDto(
                        log.getLogId(),
                        log.getNewsArticleId(),
                        log.getActorId(),
                        log.getActorName(),
                        log.getAction(),
                        log.getFromStatus(),
                        log.getToStatus(),
                        log.getNotes(),
                        log.getCreatedAt()
                ))
                .toList();
    }
}

