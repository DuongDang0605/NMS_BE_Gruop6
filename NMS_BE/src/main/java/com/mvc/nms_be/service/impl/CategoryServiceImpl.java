package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.response.CategoryTreeDto;
import com.mvc.nms_be.entity.Category;
import com.mvc.nms_be.repository.CategoryRepository;
import com.mvc.nms_be.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryTreeDto> getTree() {
        // Lấy tất cả danh mục gốc rồi đệ quy build children
        return categoryRepository
                .findByParentCategoryIsNullOrderByCategoryNameAsc()
                .stream()
                .map(this::toTreeDto)
                .toList();
    }

    private CategoryTreeDto toTreeDto(Category category) {
        List<CategoryTreeDto> children = categoryRepository
                .findByParentCategory_CategoryIdOrderByCategoryNameAsc(category.getCategoryId())
                .stream()
                .map(this::toTreeDto)
                .toList();

        return new CategoryTreeDto(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getCategoryDescription(),
                category.getIsActive(),
                children);
    }
}
