package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.CategoryRequestDto;
import com.mvc.nms_be.dto.response.CategoryResponseDto;
import com.mvc.nms_be.dto.response.CategoryTreeDto;
import com.mvc.nms_be.entity.Category;
import com.mvc.nms_be.repository.CategoryRepository;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.service.CategoryService;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final NewsArticleRepository newsArticleRepository;

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

    @Override
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public CategoryResponseDto getCategoryById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id));
        return toResponseDto(category);
    }

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto request) {
        if (request == null || request.getCategoryName() == null || request.getCategoryName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name must not be blank");
        }
        Category category = new Category();
        category.setCategoryName(request.getCategoryName().trim());
        category.setCategoryDescription(request.getCategoryDescription());
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : 1);

        if (request.getParentCategoryId() != null) {
            Category parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent category not found"));
            category.setParentCategory(parent);
        }

        Category saved = categoryRepository.save(category);
        return toResponseDto(saved);
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Integer id, CategoryRequestDto request) {
        if (request == null || request.getCategoryName() == null || request.getCategoryName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name must not be blank");
        }
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id));

        category.setCategoryName(request.getCategoryName().trim());
        category.setCategoryDescription(request.getCategoryDescription());
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        if (request.getParentCategoryId() != null) {
            if (request.getParentCategoryId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent category not found"));
            Set<Integer> visited = new HashSet<>();
            Category ancestor = parent;
            while (ancestor != null) {
                if (ancestor.getCategoryId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Cannot select a descendant as parent");
                }
                if (!visited.add(ancestor.getCategoryId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Parent hierarchy contains a cycle");
                }
                ancestor = ancestor.getParentCategory();
            }
            category.setParentCategory(parent);
        } else {
            category.setParentCategory(null);
        }

        Category updated = categoryRepository.save(category);
        return toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id);
        }
        if (categoryRepository.existsByParentCategory_CategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete category that has subcategories");
        }
        if (newsArticleRepository.existsByCategory_CategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete category that has associated articles");
        }
        categoryRepository.deleteById(id);
    }

    private CategoryResponseDto toResponseDto(Category category) {
        Integer parentId = category.getParentCategory() != null ? category.getParentCategory().getCategoryId() : null;
        return new CategoryResponseDto(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getCategoryDescription(),
                parentId,
                category.getIsActive());
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
