package com.mvc.nms_be.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicArticleDto {

    private Integer articleId;
    private String title;
    private String headline;
    private String content;
    private String source;
    private String categoryName;
    private String authorName;
    private LocalDateTime publishedDate;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isLikedByCurrentUser;
    private List<String> tags;
}
