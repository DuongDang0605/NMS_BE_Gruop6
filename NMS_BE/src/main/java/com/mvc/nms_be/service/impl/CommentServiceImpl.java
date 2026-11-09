package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.response.CommentNodeDto;
import com.mvc.nms_be.dto.response.CommentResponseDto;
import com.mvc.nms_be.dto.request.CreateCommentRequestDto;
import com.mvc.nms_be.dto.response.ProfanityCheckResult;
import com.mvc.nms_be.dto.request.ReportCommentRequestDto;
import com.mvc.nms_be.entity.Comment;
import com.mvc.nms_be.entity.CommentReport;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.exception.ResourceNotFoundException;
import com.mvc.nms_be.mapper.CommentMapper;
import com.mvc.nms_be.repository.CommentReportRepository;
import com.mvc.nms_be.repository.CommentRepository;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.repository.SystemAccountRepository;
import com.mvc.nms_be.security.CustomUserDetails;
import com.mvc.nms_be.security.SecurityUtils;
import com.mvc.nms_be.service.CommentService;
import com.mvc.nms_be.service.ModerationSettingService;
import com.mvc.nms_be.service.ProfanityFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final SystemAccountRepository systemAccountRepository;
    private final ProfanityFilterService profanityFilterService;
    private final ModerationSettingService moderationSettingService;
    private final CommentMapper commentMapper;

    // ------------------------------------------------------------------ Cây bình luận

    @Override
    @Transactional(readOnly = true)
    public List<CommentNodeDto> getArticleComments(Integer articleId) {
        NewsArticle article = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại: " + articleId));
        if (!isPubliclyPublished(article)) {
            // Giữ cùng hành vi 404 với public article API, không lộ bình luận của bài đã ẩn.
            throw new ResourceNotFoundException("Bài viết không tồn tại: " + articleId);
        }

        List<Comment> comments = commentRepository.findTreeSource(
                articleId, List.of(Comment.STATUS_APPROVED, Comment.STATUS_DELETED));

        Map<Integer, CommentNodeDto> nodes = new HashMap<>();
        for (Comment c : comments) {
            nodes.put(c.getCommentId(), commentMapper.toNode(c));
        }

        List<CommentNodeDto> roots = new ArrayList<>();
        for (Comment c : comments) {
            CommentNodeDto node = nodes.get(c.getCommentId());
            if (node.parentCommentId() == null) {
                roots.add(node);
            } else {
                CommentNodeDto parent = nodes.get(node.parentCommentId());
                if (parent != null) {
                    parent.replies().add(node);
                }
                // Bình luận cha không còn hiển thị (bị từ chối/chờ duyệt) thì nhánh con bị ẩn theo.
            }
        }

        roots.removeIf(root -> !isVisible(root));
        return roots;
    }




    /**
     * Một nút DELETED chỉ được giữ lại khi còn ít nhất một câu trả lời hiển thị bên dưới,
     * để không làm đứt mạch thảo luận. Hàm đồng thời loại các nhánh con không hiển thị.
     */
    private boolean isVisible(CommentNodeDto node) {
        node.replies().removeIf(reply -> !isVisible(reply));
        return !Comment.STATUS_DELETED.equals(node.status()) || !node.replies().isEmpty();
    }


    // ------------------------------------------------------------------ Đăng bình luận

    @Override
    @Transactional
    public CommentResponseDto postComment(CreateCommentRequestDto request) {
        Integer currentUserId = SecurityUtils.getCurrentAccountId();
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();

        // 1. Kiểm tra bài viết và quyền mở bình luận
        NewsArticle article = newsArticleRepository.findById(request.newsArticleId())
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại: " + request.newsArticleId()));

        String content = request.content().trim();
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Nội dung bình luận tối đa 2000 ký tự");
        }
        if (!isPubliclyPublished(article)) {
            throw new IllegalArgumentException("Bài viết hiện không khả dụng để bình luận");
        }
        if (Boolean.FALSE.equals(article.getAllowComments())) {
            throw new IllegalArgumentException("Bài viết này đã tắt tính năng bình luận");
        }

        // BLOCK từ chối ngay; FLAG cho phép lưu nhưng bắt buộc moderator duyệt.
        ProfanityCheckResult check = profanityFilterService.checkContent(content);
        if (check.hasBlockedWord()) {
            throw new IllegalArgumentException(
                    "Bình luận chứa từ ngữ không phù hợp và bị từ chối: " + check.detectedWords());
        }

        // 3. Bình luận cha (nếu là trả lời)
        Comment parent = null;
        if (request.parentCommentId() != null) {
            parent = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bình luận cha không tồn tại"));
            if (!parent.getNewsArticleId().equals(article.getNewsArticleId())) {
                throw new IllegalArgumentException("Bình luận cha không thuộc bài viết này");
            }
            if (!Comment.STATUS_APPROVED.equals(parent.getStatus())) {
                throw new IllegalArgumentException("Không thể trả lời bình luận này");
            }
        }

        // 4. Khởi tạo bình luận
        Comment comment = new Comment();
        comment.setNewsArticleId(article.getNewsArticleId());
        comment.setAccount(systemAccountRepository.getReferenceById(currentUserId));
        comment.setParentComment(parent);
        comment.setContent(content);
        comment.setIsFlaggedProfanity(check.hasFlaggedWord());

        // Chỉ tài khoản đã xác minh, nội dung sạch và chế độ POST mới được hiển thị ngay.
        boolean mustModerate = !Boolean.TRUE.equals(currentUser.isVerified())
                || check.hasFlaggedWord()
                || moderationSettingService.isGlobalPreModeration();
        comment.setStatus(mustModerate
                ? Comment.STATUS_PENDING_MODERATION
                : Comment.STATUS_APPROVED);

        Comment saved = commentRepository.save(comment);

        // commentCount chỉ đếm nội dung công khai, không tính comment đang chờ duyệt.
        if (!mustModerate) {
            commentRepository.adjustArticleCommentCount(article.getNewsArticleId(), 1);
        }
        return commentMapper.toDto(saved);
    }

    /**
     * Bài cũ có thể còn trạng thái "1"; public portal cũng coi giá trị này là PUBLISHED.
     */
    private boolean isPubliclyPublished(NewsArticle article) {
        return NewsArticle.STATUS_PUBLISHED.equals(article.getNewsStatus())
                || "1".equals(article.getNewsStatus());
    }

    // ------------------------------------------------------------------ Báo cáo vi phạm

    @Override
    @Transactional
    public void reportComment(Integer commentId, ReportCommentRequestDto request) {
        Integer currentUserId = SecurityUtils.getCurrentAccountId();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bình luận không tồn tại: " + commentId));

        if (!Comment.STATUS_APPROVED.equals(comment.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể báo cáo bình luận đang hiển thị");
        }
        if (currentUserId.equals(comment.getAccount().getAccountId())) {
            throw new IllegalArgumentException("Bạn không thể báo cáo bình luận của chính mình");
        }
        if (commentReportRepository.existsByCommentAndReporter(commentId, currentUserId)) {
            throw new IllegalArgumentException("Bạn đã báo cáo bình luận này rồi");
        }

        String note = request.note() == null ? "" : request.note().trim();
        String reason = note.isEmpty() ? request.reason() : request.reason() + ": " + note;

        CommentReport report = new CommentReport();
        report.setComment(comment);
        report.setReporter(systemAccountRepository.getReferenceById(currentUserId));
        report.setReason(reason);
        commentReportRepository.save(report);

        // Tăng nguyên tử để không mất lượt đếm khi nhiều người báo cáo cùng lúc
        commentRepository.incrementReportCount(commentId);
    }
}
