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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "ArticleReaction",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_Article_Account_Reaction",
                columnNames = {"NewsArticleID", "AccountID", "ReactionType"}),
        indexes = {
                @Index(name = "IX_ArticleReaction_Article", columnList = "NewsArticleID"),
                @Index(name = "IX_ArticleReaction_Account", columnList = "AccountID")
        })
@Getter
@Setter
@NoArgsConstructor
public class ArticleReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReactionID")
    private Integer reactionId;

    @Column(name = "NewsArticleID", nullable = false)
    private Integer newsArticleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AccountID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_ArticleReaction_Account"))
    private SystemAccount account;

    @Column(name = "ReactionType", nullable = false, length = 20)
    private String reactionType = "LIKE";

    @Column(name = "CreatedDate", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    private void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
