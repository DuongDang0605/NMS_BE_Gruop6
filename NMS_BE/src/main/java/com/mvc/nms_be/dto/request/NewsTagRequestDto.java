package com.mvc.nms_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsTagRequestDto {

    private Integer newsArticleId;
    private Integer tagId;
}
