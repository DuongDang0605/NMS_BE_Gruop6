package com.mvc.nms_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrendingArticleDto {

    private Integer articleId;
    private String title;
    private Integer viewCount;
    private Integer likeCount;
}
