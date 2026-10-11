package com.mvc.nms_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDto {

    private String categoryName;
    private String categoryDescription;
    private Integer parentCategoryId;
    private Integer isActive = 1;
}
