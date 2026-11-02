package com.mvc.nms_be.mapper;

import com.mvc.nms_be.dto.response.BannedKeywordResponseDto;
import com.mvc.nms_be.dto.response.CommentNodeDto;
import com.mvc.nms_be.dto.response.CommentReportResponseDto;
import com.mvc.nms_be.dto.response.CommentResponseDto;
import com.mvc.nms_be.entity.BannedKeyword;
import com.mvc.nms_be.entity.Comment;
import com.mvc.nms_be.entity.CommentReport;
import com.mvc.nms_be.entity.SystemAccount;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Mapper viết tay (không phụ thuộc MapStruct). Gọi trong @Transactional
 * vì có truy cập quan hệ LAZY (account, parentComment, reporter).
 */

@Component
public class CommentMapper {

    public static final String DELETED_PLACEHOLDER = "[Bình luận này đã bị xóa bởi kiểm duyệt viên]";

    public CommentResponseDto toDto(Comment c) {
        return new CommentResponseDto(
                c.getCommentId(),
                c.getNewsArticleId(),
                c.getAccount().getAccountId(),
                authorName(c.getAccount()),
                c.getParentComment() != null ? c.getParentComment().getCommentId() : null,
                c.getContent(),
                c.getStatus(),
                c.getIsFlaggedProfanity(),
                c.getReportCount(),
                c.getCreatedDate(),
                c.getUpdatedDate()
        );
    }

    /** Dựng một nút của cây bình luận; replies rỗng, service sẽ gắn con vào sau. */
    public CommentNodeDto toNode(Comment c) {
        boolean deleted = Comment.STATUS_DELETED.equals(c.getStatus());
        return new CommentNodeDto(
                c.getCommentId(),
                c.getAccount().getAccountId(),
                authorName(c.getAccount()),
                c.getParentComment() != null ? c.getParentComment().getCommentId() : null,
                deleted ? DELETED_PLACEHOLDER : c.getContent(),
                c.getStatus(),
                c.getCreatedDate(),
                new ArrayList<>()
        );
    }

    public CommentReportResponseDto toReportDto(CommentReport r) {
        Comment c = r.getComment();
        return new CommentReportResponseDto(
                r.getReportId(),
                c.getCommentId(),
                c.getContent(),
                c.getStatus(),
                c.getReportCount(),
                r.getReporter().getAccountId(),
                authorName(r.getReporter()),
                r.getReason(),
                r.getStatus(),
                r.getResolvedBy() != null ? r.getResolvedBy().getAccountId() : null,
                r.getCreatedDate(),
                r.getResolvedDate()
        );
    }

    public BannedKeywordResponseDto toKeywordDto(BannedKeyword k) {
        return new BannedKeywordResponseDto(
                k.getKeywordId(),
                k.getWord(),
                k.getSeverity(),
                k.getIsActive(),
                k.getCreatedDate()
        );
    }

    private String authorName(SystemAccount account) {
        return account.getAccountName();
    }
}