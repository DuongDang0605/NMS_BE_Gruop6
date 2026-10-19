package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.NewsArticle;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Integer> {

    // Dùng khi xóa category: còn bài viết thuộc danh mục thì không cho xóa.
    boolean existsByCategory_CategoryId(Integer categoryId);

    long countByNewsStatus(Integer newsStatus);

    long countByCreatedDateGreaterThanEqual(LocalDateTime start);

    long countByCreatedDateGreaterThanEqualAndCreatedDateLessThan(
            LocalDateTime start, LocalDateTime end);

    @Query("SELECT c.categoryId, c.categoryName, COUNT(a) "
            + "FROM NewsArticle a JOIN a.category c "
            + "GROUP BY c.categoryId, c.categoryName ORDER BY c.categoryName")
    List<Object[]> countArticlesByCategory();
}
