package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Kiểm tra còn danh mục con hay không trước khi xóa
    boolean existsByParentCategory_CategoryId(Integer parentId);
}
