package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.ArticleReaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleReactionRepository extends JpaRepository<ArticleReaction, Integer> {

    Optional<ArticleReaction> findByNewsArticleIdAndAccount_AccountId(
            Integer newsArticleId, Integer accountId);

    boolean existsByNewsArticleIdAndAccount_AccountId(
            Integer newsArticleId, Integer accountId);
}
