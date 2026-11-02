package com.mvc.nms_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ArticleReviewNote", indexes = {
        @Index(name = "IX_ReviewNote_Article", columnList = "NewsArticleID"),
        @Index(name = "IX_ReviewNote_Editor", columnList = "EditorID")
})
@Getter
@Setter
@NoArgsConstructor
public class ArticleReviewNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NoteID")
    private Integer noteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "NewsArticleID", nullable = false, foreignKey = @ForeignKey(name = "FK_ReviewNote_Article"))
    private NewsArticle newsArticle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "EditorID", nullable = false, foreignKey = @ForeignKey(name = "FK_ReviewNote_Editor"))
    private SystemAccount editor;

    @Column(name = "FeedbackContent", nullable = false, columnDefinition = "nvarchar(max)")
    private String feedbackContent;

    @Column(name = "ActionType", nullable = false, length = 50)
    private String actionType; // CHANGES_REQUESTED, REJECTED, APPROVE_NOTE

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate;

    @PrePersist
    private void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}

