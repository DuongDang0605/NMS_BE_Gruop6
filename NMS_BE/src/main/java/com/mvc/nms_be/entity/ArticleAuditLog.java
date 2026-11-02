package com.mvc.nms_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ArticleAuditLog", indexes = {
        @Index(name = "IX_ArticleAuditLog_Article", columnList = "NewsArticleID, CreatedAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class ArticleAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogID")
    private Long logId;

    @Column(name = "NewsArticleID", nullable = false)
    private Integer newsArticleId;

    @Column(name = "ActorID", nullable = false)
    private Integer actorId;

    @Column(name = "ActorName", nullable = false, length = 100, columnDefinition = "nvarchar(100)")
    private String actorName;

    @Column(name = "Action", nullable = false, length = 50)
    private String action; // CREATE_DRAFT, UPDATE_DRAFT, SUBMIT_REVIEW, REQUEST_CHANGES, APPROVE, SCHEDULE, PUBLISH, AUTO_PUBLISH, ARCHIVE, RESTORE

    @Column(name = "FromStatus", length = 30)
    private String fromStatus;

    @Column(name = "ToStatus", nullable = false, length = 30)
    private String toStatus;

    @Column(name = "Notes", columnDefinition = "nvarchar(max)")
    private String notes;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}

