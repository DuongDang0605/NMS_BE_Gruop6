package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.CategoryRequestDto;
import com.mvc.nms_be.dto.response.CategoryResponseDto;
import com.mvc.nms_be.entity.Category;
import com.mvc.nms_be.exception.ResourceConflictException;
import com.mvc.nms_be.exception.ResourceNotFoundException;
import com.mvc.nms_be.mapper.CategoryMapper;
import com.mvc.nms_be.repository.CategoryRepository;
import com.mvc.nms_be.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Lỗi dữ liệu đầu vào (400) do @Valid + annotation trên CategoryRequestDto xử lý trước khi vào service.
// Service chỉ ném exception của nhóm: ResourceNotFoundException (404), ResourceConflictException (409).
@Service
@RequiredArgsConstructor                 // Lombok tạo constructor để Spring inject các field final
@Transactional(readOnly = true)          // Mặc định chỉ đọc; hàm ghi dữ liệu gắn @Transactional riêng
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponseDto> findAll() {
        // Lấy toàn bộ entity rồi map sang DTO, không trả entity trực tiếp ra API
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
        request.setCategoryName(request.getCategoryName().strip());
        // parentCategoryId = null nghĩa là danh mục gốc
        Category parent = findParent(request.getParentCategoryId());
        Category category = categoryMapper.toEntity(request, parent);
        return categoryMapper.toResponseDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponseDto update(Integer id, CategoryRequestDto request) {
        Category category = findCategory(id);
        Category parent = findParent(request.getParentCategoryId());
        checkNoCycle(category, parent);

        // Sửa trên entity đang có để giữ nguyên ID; Hibernate tự UPDATE khi transaction kết thúc
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
        // Kiểm tra trước để trả 409 dễ hiểu thay vì lỗi khóa ngoại (FK) từ SQL Server
        if (categoryRepository.existsByParentCategory_CategoryId(id)) {
            throw new ResourceConflictException(describe(category)
                    + " still has child categories. Delete or move its child categories first.");
        }
        if (categoryRepository.hasArticles(id)) {
            throw new ResourceConflictException(describe(category)
                    + " is used by news articles. Move those articles to another category first.");
        }
        categoryRepository.delete(category);
    }

    // ===== Hàm hỗ trợ =====

    // Tìm danh mục theo ID, không có thì 404
    private Category findCategory(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " was not found"));
    }

    // null -> danh mục gốc; có ID thì phải tồn tại, không có thì 404
    private Category findParent(Integer parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parent category with id " + parentId + " was not found"));
    }

    // Đi ngược từ cha mới lên tới gốc; gặp lại chính danh mục đang sửa thì là vòng lặp (A -> B -> A).
    // Trả 409 kèm field "parentCategoryId" để client biết sai ở trường nào.
    private static void checkNoCycle(Category category, Category parent) {
        if (parent == null) {
            return;
        }
        if (parent.getCategoryId().equals(category.getCategoryId())) {
            throw new ResourceConflictException(describe(category)
                    + " cannot be its own parent.", "parentCategoryId");
        }
        for (Category current = parent.getParentCategory(); current != null; current = current.getParentCategory()) {
            if (current.getCategoryId().equals(category.getCategoryId())) {
                throw new ResourceConflictException("Cannot set " + describe(parent) + " as parent of "
                        + describe(category) + " because it is a descendant of " + describe(category)
                        + ". This would create a cycle.", "parentCategoryId");
            }
        }
    }

    // Ví dụ: Category 'Thể thao' (id 1) — dùng trong message để client biết chính xác danh mục nào
    private static String describe(Category category) {
        return "Category '" + category.getCategoryName() + "' (id " + category.getCategoryId() + ")";
    }
}
