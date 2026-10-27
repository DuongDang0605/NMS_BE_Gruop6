package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.response.CategoryTreeDto;
import com.mvc.nms_be.entity.Category;
import com.mvc.nms_be.repository.CategoryRepository;
import com.mvc.nms_be.service.CategoryService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        // Load toàn bộ category 1 query duy nhất, build cây trong memory — tránh N+1
        List<Category> all = categoryRepository.findAll();

        // Nhóm các category con theo parentId
        Map<Integer, List<Category>> byParent = all.stream()
                .filter(c -> c.getParentCategory() != null)
                .collect(Collectors.groupingBy(c -> c.getParentCategory().getCategoryId()));

        // Chỉ lấy root (không có cha) rồi đệ quy build từ map đã có sẵn
        return all.stream()
                .filter(c -> c.getParentCategory() == null)
                .sorted(Comparator.comparing(Category::getCategoryName))
                .map(c -> toTreeDto(c, byParent))
                .toList();
    }

    private CategoryTreeDto toTreeDto(Category category, Map<Integer, List<Category>> byParent) {
        List<CategoryTreeDto> children = byParent
                .getOrDefault(category.getCategoryId(), List.of())
                .stream()
                .sorted(Comparator.comparing(Category::getCategoryName))
                .map(c -> toTreeDto(c, byParent))
                .toList();

        return new CategoryTreeDto(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getCategoryDescription(),
                category.getIsActive(),
                children);
    }
}
