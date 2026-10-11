package com.mvc.nms_be.mapper;

import com.mvc.nms_be.dto.request.CategoryRequestDto;
import com.mvc.nms_be.dto.response.CategoryResponseDto;
import com.mvc.nms_be.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    // The service resolves request.parentCategoryId; root categories use null.
    public Category toEntity(CategoryRequestDto request, Category parentCategory) {
        Category category = new Category();
        category.setCategoryName(request.getCategoryName());
        category.setCategoryDescription(request.getCategoryDescription());
        category.setParentCategory(parentCategory);
        category.setIsActive(request.getIsActive());
        return category;
    }

    public CategoryResponseDto toResponseDto(Category category) {
        return new CategoryResponseDto(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getCategoryDescription(),
                category.getParentCategory() == null ? null : category.getParentCategory().getCategoryId(),
                category.getIsActive()
        );
    }
}
