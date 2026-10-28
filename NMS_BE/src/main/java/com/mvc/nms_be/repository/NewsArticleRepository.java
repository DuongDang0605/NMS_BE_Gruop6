package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.NewsArticle;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // dùng khi xóa category: còn bài viết thuộc danh mục thì không cho xóa
    boolean existsByCategory_CategoryId(Integer categoryId);

    long countByNewsStatus(Integer newsStatus);

    long countByCreatedDateGreaterThanEqual(LocalDateTime start);

    long countByCreatedDateGreaterThanEqualAndCreatedDateLessThan(
            LocalDateTime start, LocalDateTime end);

    @Query("SELECT c.categoryId, c.categoryName, COUNT(a) " +
            "FROM NewsArticle a JOIN a.category c " +
            "GROUP BY c.categoryId, c.categoryName ORDER BY c.categoryName")
    List<Object[]> countArticlesByCategory();

    // dashboard news: mỗi dòng là 1 ngày UTC (yyyy-MM-dd), kèm số bài trong ngày và số bài thuộc cửa sổ 7 ngày
    @Query(value = "SELECT CONVERT(char(10), CreatedDate, 23) AS CreatedDay, "
            + "COUNT_BIG(*) AS DayCount, "
            + "COUNT_BIG(CASE WHEN CreatedDate >= :since7 THEN 1 END) AS Last7Count "
            + "FROM NewsArticle WHERE CreatedDate >= :since30 "
            + "GROUP BY CONVERT(char(10), CreatedDate, 23)",
            nativeQuery = true)
    List<Object[]> countArticlesPerDaySince(@Param("since30") LocalDateTime since30,
                                            @Param("since7") LocalDateTime since7);

    // top 5 bài viết có lượt xem cao nhất (trending) trong trạng thái đã xuất bản - Spring Data JPA derived query
    List<NewsArticle> findTop5ByNewsStatusOrderByViewCountDescLikeCountDesc(Integer newsStatus);

    // tổng ViewCount và tổng LikeCount toàn hệ thống cho Dashboard
    @Query(value = "SELECT ISNULL(SUM(ViewCount), 0), ISNULL(SUM(LikeCount), 0) FROM NewsArticle",
            nativeQuery = true)
    List<Object[]> sumViewsAndLikes();

    // tăng ViewCount nguyên tử, tránh race condition khi nhiều user đọc cùng lúc
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE NewsArticle SET ViewCount = ViewCount + 1 WHERE NewsArticleID = :articleId",
            nativeQuery = true)
    int incrementViewCount(@Param("articleId") Integer articleId);

    // public portal: lấy bài đã active (newsStatus=1) theo keyword, category, tag, phân trang bằng Pageable
    @EntityGraph(attributePaths = {"category", "createdBy"})
    @Query("SELECT DISTINCT a FROM NewsArticle a " +
            "LEFT JOIN a.newsTags nt " +
            "WHERE a.newsStatus = 1 " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(a.newsTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(a.headline) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR a.category.categoryId = :categoryId) " +
            "AND (:tagId IS NULL OR nt.tag.tagId = :tagId)")
    Page<NewsArticle> findPublishedArticles(@Param("keyword") String keyword,
                                            @Param("categoryId") Integer categoryId,
                                            @Param("tagId") Integer tagId,
                                            Pageable pageable);
}