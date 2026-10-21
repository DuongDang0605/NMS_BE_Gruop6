package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Derived query: Spring đọc tên hàm và tự sinh SQL
    // -> có danh mục con nào nhận categoryId làm cha không?
    boolean existsByParentCategory_CategoryId(Integer categoryId);

    // JPQL viết theo tên class/thuộc tính Java: có bài viết nào thuộc danh mục này không?
    // Đặt ở đây để không phải sửa NewsArticleRepository (file của người khác).
    @Query("SELECT COUNT(a) > 0 FROM NewsArticle a WHERE a.category.categoryId = :categoryId")
    boolean hasArticles(@Param("categoryId") Integer categoryId);
}
