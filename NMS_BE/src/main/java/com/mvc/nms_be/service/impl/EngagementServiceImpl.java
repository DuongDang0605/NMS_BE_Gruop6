package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.response.ReactionResponseDto;
import com.mvc.nms_be.dto.response.ViewCountResponseDto;
import com.mvc.nms_be.entity.ArticleReaction;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.exception.ResourceNotFoundException;
import com.mvc.nms_be.repository.ArticleReactionRepository;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.repository.SystemAccountRepository;
import com.mvc.nms_be.security.SecurityUtils;
import com.mvc.nms_be.service.EngagementService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EngagementServiceImpl implements EngagementService {

    private final NewsArticleRepository newsArticleRepository;
    private final ArticleReactionRepository reactionRepository;
    private final SystemAccountRepository accountRepository;

    @Override
    @Transactional
    public ReactionResponseDto toggleReaction(Integer articleId) {
        NewsArticle article = findArticle(articleId);
        Integer currentUserId = SecurityUtils.getCurrentUser().getAccountId();

        Optional<ArticleReaction> existing =
                reactionRepository.findByNewsArticleIdAndAccount_AccountId(articleId, currentUserId);

        boolean isLikedNow;
        if (existing.isPresent()) {
            // Đã like → hủy like
            reactionRepository.delete(existing.get());
            article.setLikeCount(Math.max(0, article.getLikeCount() - 1));
            isLikedNow = false;
        } else {
            // Chưa like → thêm mới
            SystemAccount account = accountRepository.getReferenceById(currentUserId);
            ArticleReaction reaction = new ArticleReaction();
            reaction.setNewsArticleId(articleId);
            reaction.setAccount(account);
            reaction.setReactionType("LIKE");
            reactionRepository.save(reaction);

            article.setLikeCount(article.getLikeCount() + 1);
            isLikedNow = true;
        }

        newsArticleRepository.save(article);
        return new ReactionResponseDto(articleId, article.getLikeCount(), isLikedNow);
    }

    @Override
    @Transactional
    public ViewCountResponseDto incrementViewCount(Integer articleId) {
        // findArticle ném 404 nếu không tồn tại, bỏ existsById thừa
        NewsArticle article = findArticle(articleId);
        // UPDATE nguyên tử trực tiếp trên DB, tránh race condition
        reactionRepository.incrementViewCount(articleId);
        // Trả về giá trị tính toán thay vì query lại lần 3
        return new ViewCountResponseDto(articleId, article.getViewCount() + 1);
    }

    private NewsArticle findArticle(Integer articleId) {
        return newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("News article not found: " + articleId));
    }
}
