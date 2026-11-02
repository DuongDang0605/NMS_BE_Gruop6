package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.ArticleReviewNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleReviewNoteRepository extends JpaRepository<ArticleReviewNote, Integer> {

    @Query("SELECT n FROM ArticleReviewNote n JOIN FETCH n.editor WHERE n.newsArticle.newsArticleId = :articleId ORDER BY n.createdDate DESC")
    List<ArticleReviewNote> findByNewsArticleIdWithEditorOrderByCreatedDateDesc(@Param("articleId") Integer articleId);

    List<ArticleReviewNote> findByNewsArticle_NewsArticleIdOrderByCreatedDateDesc(Integer newsArticleId);

    void deleteByNewsArticle_NewsArticleId(Integer newsArticleId);
}

