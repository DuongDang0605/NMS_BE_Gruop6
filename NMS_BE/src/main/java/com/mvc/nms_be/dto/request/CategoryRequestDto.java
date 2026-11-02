package com.mvc.nms_be.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDto {

    @NotBlank(message = "Category name must not be blank")
    @Size(max = 100, message = "Category name must contain at most 100 characters")
    private String categoryName;

    @Size(max = 1000, message = "Category description must contain at most 1000 characters")
    private String categoryDescription;

    // null = danh mục gốc
    private Integer parentCategoryId;

    @NotNull(message = "isActive is required")
    @Min(value = 0, message = "isActive must be 0 (inactive) or 1 (active)")
    @Max(value = 1, message = "isActive must be 0 (inactive) or 1 (active)")
    private Integer isActive;
}
