package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.CategoryRequestDto;
import com.mvc.nms_be.dto.response.CategoryResponseDto;
import com.mvc.nms_be.entity.Category;
import com.mvc.nms_be.mapper.CategoryMapper;
import com.mvc.nms_be.repository.CategoryRepository;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor                 // Lombok tạo constructor để Spring inject các field final
@Transactional(readOnly = true)          // Mặc định chỉ đọc; method ghi dữ liệu gắn @Transactional riêng
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final NewsArticleRepository articleRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponseDto> findAll() {
        // Lấy toàn bộ entity rồi map sang DTO, không trả entity trực tiếp ra API.
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponseDto)
                .toList();
    }

    @Override
    public CategoryResponseDto getById(Integer id) {
        return categoryMapper.toResponseDto(findCategory(id));
    }

    @Override
    @Transactional
    public CategoryResponseDto create(CategoryRequestDto request) {
        validate(request);
        request.setCategoryName(request.getCategoryName().strip());
        // parentCategoryId = null nghĩa là danh mục gốc.
        Category parent = findParent(request.getParentCategoryId());
        Category category = categoryMapper.toEntity(request, parent);
        return categoryMapper.toResponseDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponseDto update(Integer id, CategoryRequestDto request) {
        validate(request);
        Category category = findCategory(id);
        Category parent = findParent(request.getParentCategoryId());
        checkNoCycle(category, parent);

        // Cập nhật trên entity đang có để giữ nguyên ID; Hibernate tự UPDATE khi transaction kết thúc.
        category.setCategoryName(request.getCategoryName().strip());
        category.setCategoryDescription(request.getCategoryDescription());
        category.setParentCategory(parent);
        category.setIsActive(request.getIsActive());
        return categoryMapper.toResponseDto(category);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Category category = findCategory(id);
        // Chặn trước để trả lỗi 409 dễ hiểu thay vì lỗi khóa ngoại (FK) từ SQL Server.
        if (categoryRepository.existsByParentCategory_CategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category still has child categories");
        }
        if (articleRepository.existsByCategory_CategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category is used by news articles");
        }
        categoryRepository.delete(category);
    }

    // ===== Hàm hỗ trợ =====

    private Category findCategory(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private Category findParent(Integer parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent category not found"));
    }

    // Kiểm tra dữ liệu đầu vào cơ bản, sai thì trả 400 Bad Request.
    private static void validate(CategoryRequestDto request) {
        if (request == null || request.getCategoryName() == null || request.getCategoryName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryName must not be blank");
        }
        if (request.getCategoryName().strip().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryName must contain at most 100 characters");
        }
        if (request.getCategoryDescription() != null && request.getCategoryDescription().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryDescription must contain at most 1000 characters");
        }
        if (request.getIsActive() == null || (request.getIsActive() != 0 && request.getIsActive() != 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isActive must be 0 or 1");
        }
    }

    // Không cho category làm cha của chính nó hoặc của tổ tiên nó (vòng lặp A -> B -> A).
    private static void checkNoCycle(Category category, Category parent) {
        for (Category current = parent; current != null; current = current.getParentCategory()) {
            if (current.getCategoryId().equals(category.getCategoryId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent category creates a cycle");
            }
        }
    }
}
