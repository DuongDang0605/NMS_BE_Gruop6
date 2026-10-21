package com.mvc.nms_be.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "NewsArticle", indexes = {
        @Index(name = "IX_NewsArticle_Category", columnList = "CategoryID"),
        @Index(name = "IX_NewsArticle_Creator", columnList = "CreatedByID"),
        @Index(name = "IX_NewsArticle_Updater", columnList = "UpdatedByID"),
        @Index(name = "IX_NewsArticle_CreatedDate", columnList = "CreatedDate")
})
@Check(name = "CK_NewsArticle_Status", constraints = "NewsStatus IN (0, 1)")
@Getter
@Setter
@NoArgsConstructor
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NewsArticleID")
    private Integer newsArticleId;

    @Column(name = "NewsTitle", nullable = false, length = 250, columnDefinition = "nvarchar(250)")
    private String newsTitle;

    @Column(name = "Headline", length = 500, columnDefinition = "nvarchar(500)")
    private String headline;

    @Column(name = "CreatedDate", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "NewsContent", nullable = false, columnDefinition = "nvarchar(max)")
    private String newsContent;

    @Column(name = "NewsSource", length = 250, columnDefinition = "nvarchar(250)")
    private String newsSource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CategoryID", nullable = false, foreignKey = @ForeignKey(name = "FK_NewsArticle_Category"))
    private Category category;

    @Column(name = "NewsStatus", nullable = false)
    private Integer newsStatus = 1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CreatedByID", nullable = false, foreignKey = @ForeignKey(name = "FK_NewsArticle_Creator"))
    private SystemAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UpdatedByID", foreignKey = @ForeignKey(name = "FK_NewsArticle_Updater"))
    private SystemAccount updatedBy;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @OneToMany(mappedBy = "newsArticle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NewsTag> newsTags = new ArrayList<>();

    @PrePersist
    private void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    @PreUpdate
    private void onUpdate() {
        modifiedDate = LocalDateTime.now(ZoneOffset.UTC);
    }
}
