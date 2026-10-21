package com.mvc.nms_be.dto.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticleResponseDto {

    private Integer newsArticleId;
    private String newsTitle;
    private String headline;
    private LocalDateTime createdDate;
    private String newsContent;
    private String newsSource;
    private Integer categoryId;
    private String categoryName;
    private Integer newsStatus;
    private Integer createdById;
    private String createdByName;
    private Integer updatedById;
    private String updatedByName;
    private LocalDateTime modifiedDate;
    private List<Integer> tagIds;
    private List<TagResponseDto> tags;
}
