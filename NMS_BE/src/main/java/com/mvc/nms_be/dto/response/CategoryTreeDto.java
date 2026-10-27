package com.mvc.nms_be.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeDto {

    private Integer categoryId;
    private String categoryName;
    private String categoryDescription;
    private Integer isActive;
    private List<CategoryTreeDto> children;
}
