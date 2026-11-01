package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.ArticleAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleAuditLogRepository extends JpaRepository<ArticleAuditLog, Long> {

    List<ArticleAuditLog> findByNewsArticleIdOrderByCreatedAtDesc(Integer newsArticleId);

    void deleteByNewsArticleId(Integer newsArticleId);
}

