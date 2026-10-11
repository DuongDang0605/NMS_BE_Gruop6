package com.mvc.nms_be.dto.request;

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
public class NewsArticleRequestDto {

    private String newsTitle;
    private String headline;
    private String newsContent;
    private String newsSource;
    private Integer categoryId;
    private Integer newsStatus = 1;
    private List<Integer> tagIds = new ArrayList<>();
}
