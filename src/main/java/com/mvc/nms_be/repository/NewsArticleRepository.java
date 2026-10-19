package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.NewsArticle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Integer> {

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
}