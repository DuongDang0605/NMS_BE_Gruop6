package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.ArticleReaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleReactionRepository extends JpaRepository<ArticleReaction, Integer> {

    Optional<ArticleReaction> findByNewsArticleIdAndAccount_AccountId(
            Integer newsArticleId, Integer accountId);

    boolean existsByNewsArticleIdAndAccount_AccountId(
            Integer newsArticleId, Integer accountId);

    // Tăng ViewCount nguyên tử, tránh race condition khi nhiều user đọc cùng lúc
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE NewsArticle SET ViewCount = ViewCount + 1 WHERE NewsArticleID = :articleId",
            nativeQuery = true)
    int incrementViewCount(@Param("articleId") Integer articleId);
}
