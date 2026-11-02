package com.mvc.nms_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NewsTagId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "NewsArticleID")
    private Integer newsArticleId;

    @Column(name = "TagID")
    private Integer tagId;
}
