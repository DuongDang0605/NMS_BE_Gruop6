package com.mvc.nms_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {

    private Integer categoryId;
    private String categoryName;
    private String categoryDescription;
    private Integer parentCategoryId;
    private Integer isActive;
}
