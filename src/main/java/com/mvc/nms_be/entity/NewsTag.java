package com.mvc.nms_be.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "NewsTag", indexes = @Index(name = "IX_NewsTag_Tag", columnList = "TagID"))
@Getter
@Setter
@NoArgsConstructor
public class NewsTag {

    @EmbeddedId
    private NewsTagId id = new NewsTagId();

    @MapsId("newsArticleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "NewsArticleID", nullable = false, foreignKey = @ForeignKey(name = "FK_NewsTag_Article"))
    private NewsArticle newsArticle;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TagID", nullable = false, foreignKey = @ForeignKey(name = "FK_NewsTag_Tag"))
    private Tag tag;

    public NewsTag(NewsArticle newsArticle, Tag tag) {
        this.newsArticle = newsArticle;
        this.tag = tag;
        this.id = new NewsTagId(newsArticle.getNewsArticleId(), tag.getTagId());
    }
}
