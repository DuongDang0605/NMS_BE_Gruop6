package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.response.CommentReportResponseDto;
import com.mvc.nms_be.dto.response.CommentResponseDto;
import com.mvc.nms_be.entity.Comment;
import com.mvc.nms_be.entity.CommentReport;
import com.mvc.nms_be.exception.ResourceNotFoundException;
import com.mvc.nms_be.mapper.CommentMapper;
import com.mvc.nms_be.repository.CommentReportRepository;
import com.mvc.nms_be.repository.CommentRepository;
import com.mvc.nms_be.repository.SystemAccountRepository;
import com.mvc.nms_be.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Nghiệp vụ bàn làm việc của kiểm duyệt viên. Mọi hàm đổi trạng thái đều cập nhật
 * commentCount của bài viết theo trạng thái cũ, nên gọi lặp lại không làm số đếm lệch.
 */
@Service
@RequiredArgsConstructor
public class ModerationService {

    private static final Set<String> REPORT_STATUSES = Set.of(
            CommentReport.STATUS_PENDING, CommentReport.STATUS_RESOLVED, CommentReport.STATUS_DISMISSED);

    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final SystemAccountRepository systemAccountRepository;
    private final CommentMapper commentMapper;

    // ------------------------------------------------------------------ Hàng đợi & báo cáo

    /** Bình luận chờ duyệt hoặc đã bị báo cáo; bình luận bị báo cáo nhiều nằm trên đầu. */
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getPendingComments(Pageable pageable) {
        return commentRepository.findModerationQueue(pageable).map(commentMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<CommentReportResponseDto> getReportList(String status, Pageable pageable) {
        String resolved = status == null || status.isBlank() ? CommentReport.STATUS_PENDING : status.toUpperCase();
        if (!REPORT_STATUSES.contains(resolved)) {
            throw new IllegalArgumentException("Trạng thái báo cáo không hợp lệ: " + status);
        }
        return commentReportRepository.findByStatus(resolved, pageable).map(commentMapper::toReportDto);
    }

    // ------------------------------------------------------------------ Quyết định kiểm duyệt

    @Transactional
    public void approveComment(Integer commentId) {
        Comment comment = loadComment(commentId);
        String old = comment.getStatus();

        if (Comment.STATUS_APPROVED.equals(old)) {
            return; // đã hiển thị, không đếm lại
        }
        if (Comment.STATUS_DELETED.equals(old)) {
            throw new IllegalArgumentException("Không thể duyệt bình luận đã bị xóa");
        }

        changeStatus(comment, Comment.STATUS_APPROVED);
        commentRepository.adjustArticleCommentCount(comment.getNewsArticleId(), 1);
        closePendingReports(commentId, CommentReport.STATUS_RESOLVED);
    }

    @Transactional
    public void rejectComment(Integer commentId) {
        Comment comment = loadComment(commentId);
        String old = comment.getStatus();

        if (Comment.STATUS_REJECTED.equals(old)) {
            return;
        }
        if (Comment.STATUS_DELETED.equals(old)) {
            throw new IllegalArgumentException("Không thể từ chối bình luận đã bị xóa");
        }

        changeStatus(comment, Comment.STATUS_REJECTED);
        if (Comment.STATUS_APPROVED.equals(old)) {
            commentRepository.adjustArticleCommentCount(comment.getNewsArticleId(), -1);
        }
        closePendingReports(commentId, CommentReport.STATUS_RESOLVED);
    }

    @Transactional
    public void deleteComment(Integer commentId) {
        Comment comment = loadComment(commentId);
        String old = comment.getStatus();

        if (Comment.STATUS_DELETED.equals(old)) {
            return;
        }

        changeStatus(comment, Comment.STATUS_DELETED);
        if (Comment.STATUS_APPROVED.equals(old)) {
            commentRepository.adjustArticleCommentCount(comment.getNewsArticleId(), -1);
        }
        closePendingReports(commentId, CommentReport.STATUS_RESOLVED);
    }

    /** "Giữ lại": bác bỏ các báo cáo, giữ nguyên bình luận và lịch sử số lượt báo cáo. */
    @Transactional
    public void keepComment(Integer commentId) {
        Comment comment = loadComment(commentId);
        comment.setUpdatedDate(LocalDateTime.now());
        commentRepository.save(comment);
        closePendingReports(commentId, CommentReport.STATUS_DISMISSED);
    }

    // ------------------------------------------------------------------ Hàm phụ

    private Comment loadComment(Integer commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bình luận không tồn tại: " + commentId));
    }

    private void changeStatus(Comment comment, String newStatus) {
        comment.setStatus(newStatus);
        comment.setUpdatedDate(LocalDateTime.now());
        commentRepository.save(comment);
    }

    private void closePendingReports(Integer commentId, String newStatus) {
        List<CommentReport> reports = commentReportRepository
                .findByCommentAndStatus(commentId, CommentReport.STATUS_PENDING);
        if (reports.isEmpty()) {
            return;
        }
        var moderator = systemAccountRepository.getReferenceById(SecurityUtils.getCurrentAccountId());
        LocalDateTime now = LocalDateTime.now();
        for (CommentReport report : reports) {
            report.setStatus(newStatus);
            report.setResolvedBy(moderator);
            report.setResolvedDate(now);
        }
        commentReportRepository.saveAll(reports);
    }
}