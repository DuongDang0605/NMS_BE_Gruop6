package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Spring Data tự sinh câu SQL từ tên method: kiểm tra có danh mục con nào trỏ tới categoryId không.
    boolean existsByParentCategory_CategoryId(Integer categoryId);
}
