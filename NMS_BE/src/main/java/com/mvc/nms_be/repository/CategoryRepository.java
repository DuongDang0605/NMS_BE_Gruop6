package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Lấy tất cả danh mục gốc (không có cha) để build cây
    List<Category> findByParentCategoryIsNullOrderByCategoryNameAsc();

    // Lấy danh mục con theo cha để build cây từng nhánh
    List<Category> findByParentCategory_CategoryIdOrderByCategoryNameAsc(Integer parentId);

    // Kiểm tra còn danh mục con hay không trước khi xóa
    boolean existsByParentCategory_CategoryId(Integer parentId);
}
