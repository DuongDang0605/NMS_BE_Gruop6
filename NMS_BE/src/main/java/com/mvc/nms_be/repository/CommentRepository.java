package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

    /**
     * Lấy toàn bộ bình luận của một bài viết theo các trạng thái cho phép
     * (APPROVED, DELETED) để dựng cây bình luận. Fetch sẵn account tránh N+1.
     */
    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.account
            LEFT JOIN FETCH c.parentComment
            WHERE c.newsArticleId = :articleId
              AND c.status IN :statuses
            ORDER BY c.createdDate ASC
            """)
    List<Comment> findTreeSource(@Param("articleId") Integer articleId,
                                 @Param("statuses") Collection<String> statuses);

    /**
     * Hàng đợi kiểm duyệt: bình luận chờ duyệt hoặc đã bị báo cáo.
     * Ưu tiên bình luận bị báo cáo nhiều nhất lên đầu (AC-EN-003).
     * Truyền Pageable không có sort vì thứ tự đã nằm trong câu truy vấn.
     */
    @Query(value = """
            SELECT c FROM Comment c
            JOIN FETCH c.account
            WHERE c.status = 'PENDING_MODERATION'
               OR (c.reportCount > 0 AND c.status <> 'DELETED')
            ORDER BY c.reportCount DESC, c.createdDate ASC
            """,
            countQuery = """
            SELECT COUNT(c) FROM Comment c
            WHERE c.status = 'PENDING_MODERATION'
               OR (c.reportCount > 0 AND c.status <> 'DELETED')
            """)
    Page<Comment> findModerationQueue(Pageable pageable);

    boolean existsByParentCommentCommentId(Integer parentCommentId);

    long countByNewsArticleIdAndStatus(Integer newsArticleId, String status);

    /**
     * Cộng/trừ commentCount của bài viết ngay trong database (nguyên tử, không bị lệch khi
     * nhiều người thao tác cùng lúc). Không bao giờ để số đếm xuống dưới 0.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE NewsArticle a
            SET a.commentCount = a.commentCount + :delta
            WHERE a.newsArticleId = :articleId
              AND a.commentCount + :delta >= 0
            """)
    int adjustArticleCommentCount(@Param("articleId") Integer articleId, @Param("delta") int delta);

    /** Tăng reportCount nguyên tử khi có báo cáo mới. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Comment c SET c.reportCount = c.reportCount + 1 WHERE c.commentId = :commentId")
    int incrementReportCount(@Param("commentId") Integer commentId);
}