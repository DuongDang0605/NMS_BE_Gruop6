package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.CommentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Integer> {

    /** Kiểm tra user đã báo cáo bình luận này chưa (chặn báo cáo lặp). */
    @Query("""
            SELECT COUNT(r) > 0 FROM CommentReport r
            WHERE r.comment.commentId = :commentId
              AND r.reporter.accountId = :reporterId
            """)
    boolean existsByCommentAndReporter(@Param("commentId") Integer commentId,
                                       @Param("reporterId") Integer reporterId);

    /** Danh sách phiếu báo cáo theo trạng thái, mới nhất trước. */
    @Query(value = """
            SELECT r FROM CommentReport r
            JOIN FETCH r.comment c
            JOIN FETCH r.reporter
            WHERE r.status = :status
            ORDER BY r.createdDate DESC
            """,
            countQuery = "SELECT COUNT(r) FROM CommentReport r WHERE r.status = :status")
    Page<CommentReport> findByStatus(@Param("status") String status, Pageable pageable);

    /** Các phiếu còn PENDING của một bình luận, dùng khi "Giữ lại" hoặc "Xóa". */
    @Query("""
            SELECT r FROM CommentReport r
            WHERE r.comment.commentId = :commentId
              AND r.status = :status
            """)
    List<CommentReport> findByCommentAndStatus(@Param("commentId") Integer commentId,
                                               @Param("status") String status);
}