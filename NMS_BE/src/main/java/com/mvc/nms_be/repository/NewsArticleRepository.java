package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.NewsArticle;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Integer> {

    // ==================== QUERY & SEARCH METHODS ====================

    List<NewsArticle> findByCategory_CategoryId(Integer categoryId);

    List<NewsArticle> findByNewsStatus(Integer newsStatus);

    List<NewsArticle> findByCreatedBy_AccountId(Integer accountId);

    @Query("SELECT DISTINCT n FROM NewsArticle n JOIN n.newsTags nt WHERE nt.tag.tagId = :tagId")
    List<NewsArticle> findByTagId(@Param("tagId") Integer tagId);

    @Query("SELECT DISTINCT n FROM NewsArticle n LEFT JOIN n.newsTags nt WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(n.newsTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(n.headline) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(n.newsContent) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:categoryId IS NULL OR n.category.categoryId = :categoryId) AND " +
            "(:newsStatus IS NULL OR n.newsStatus = :newsStatus) AND " +
            "(:tagId IS NULL OR nt.tag.tagId = :tagId) " +
            "ORDER BY n.createdDate DESC")
    List<NewsArticle> searchArticles(@Param("keyword") String keyword,
                                     @Param("categoryId") Integer categoryId,
                                     @Param("newsStatus") Integer newsStatus,
                                     @Param("tagId") Integer tagId);

    // ==================== VALIDATION & COUNTING METHODS ====================

    // Dùng khi xóa category: còn bài viết thuộc danh mục thì không cho xóa
    boolean existsByCategory_CategoryId(Integer categoryId);

    long countByNewsStatus(Integer newsStatus);

    long countByCreatedDateGreaterThanEqual(LocalDateTime start);

    long countByCreatedDateGreaterThanEqualAndCreatedDateLessThan(
            LocalDateTime start, LocalDateTime end);

    @Query("SELECT c.categoryId, c.categoryName, COUNT(a) " +
            "FROM NewsArticle a JOIN a.category c " +
            "GROUP BY c.categoryId, c.categoryName ORDER BY c.categoryName")
    List<Object[]> countArticlesByCategory();

    // Dashboard overview: mỗi bảng tổng hợp thành đúng 1 dòng rồi CROSS JOIN, nên COUNT không bị nhân bản
    @Query(value = "SELECT a.TotalAccounts, a.TotalAdmins, a.TotalStaff, c.TotalCategories, t.TotalTags, "
            + "n.TotalArticles, n.ActiveArticles, n.InactiveArticles, nt.TotalNotifications "
            + "FROM (SELECT COUNT_BIG(*) AS TotalAccounts, "
            + "        COUNT_BIG(CASE WHEN AccountRole = :adminRole THEN 1 END) AS TotalAdmins, "
            + "        COUNT_BIG(CASE WHEN AccountRole = :staffRole THEN 1 END) AS TotalStaff "
            + "      FROM SystemAccount) a "
            + "CROSS JOIN (SELECT COUNT_BIG(*) AS TotalCategories FROM Category) c "
            + "CROSS JOIN (SELECT COUNT_BIG(*) AS TotalTags FROM Tag) t "
            + "CROSS JOIN (SELECT COUNT_BIG(*) AS TotalArticles, "
            + "        COUNT_BIG(CASE WHEN NewsStatus = 1 THEN 1 END) AS ActiveArticles, "
            + "        COUNT_BIG(CASE WHEN NewsStatus = 0 THEN 1 END) AS InactiveArticles "
            + "      FROM NewsArticle) n "
            + "CROSS JOIN (SELECT COUNT_BIG(*) AS TotalNotifications FROM Notification) nt",
            nativeQuery = true)
    List<Object[]> dashboardOverview(@Param("adminRole") Integer adminRole,
                                     @Param("staffRole") Integer staffRole);

    // Dashboard news: mỗi dòng là 1 ngày UTC (yyyy-MM-dd), kèm số bài trong ngày và số bài thuộc cửa sổ 7 ngày
    @Query(value = "SELECT CONVERT(char(10), CreatedDate, 23) AS CreatedDay, "
            + "COUNT_BIG(*) AS DayCount, "
            + "COUNT_BIG(CASE WHEN CreatedDate >= :since7 THEN 1 END) AS Last7Count "
            + "FROM NewsArticle WHERE CreatedDate >= :since30 "
            + "GROUP BY CONVERT(char(10), CreatedDate, 23)",
            nativeQuery = true)
    List<Object[]> countArticlesPerDaySince(@Param("since30") LocalDateTime since30,
                                            @Param("since7") LocalDateTime since7);
}