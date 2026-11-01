package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.NewsTag;
import com.mvc.nms_be.entity.NewsTagId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NewsTagRepository extends JpaRepository<NewsTag, NewsTagId> {

    List<NewsTag> findByNewsArticle_NewsArticleId(Integer newsArticleId);

    @Query("SELECT nt.tag.tagName FROM NewsTag nt WHERE nt.newsArticle.newsArticleId = :articleId")
    List<String> findTagNamesByArticleId(@Param("articleId") Integer articleId);

    List<NewsTag> findByTag_TagId(Integer tagId);

    boolean existsByNewsArticle_NewsArticleIdAndTag_TagId(Integer newsArticleId, Integer tagId);

    @Transactional
    @Modifying
    @Query("DELETE FROM NewsTag nt WHERE nt.newsArticle.newsArticleId = :articleId")
    void deleteByArticleId(@Param("articleId") Integer articleId);

    @Transactional
    @Modifying
    @Query("DELETE FROM NewsTag nt WHERE nt.tag.tagId = :tagId")
    void deleteByTagId(@Param("tagId") Integer tagId);

    @Transactional
    @Modifying
    @Query("DELETE FROM NewsTag nt WHERE nt.newsArticle.newsArticleId = :articleId AND nt.tag.tagId = :tagId")
    void deleteByArticleIdAndTagId(@Param("articleId") Integer articleId, @Param("tagId") Integer tagId);
}
