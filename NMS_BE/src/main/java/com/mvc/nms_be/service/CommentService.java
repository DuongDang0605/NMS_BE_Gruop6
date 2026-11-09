package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.response.CommentNodeDto;
import com.mvc.nms_be.dto.response.CommentResponseDto;
import com.mvc.nms_be.dto.request.CreateCommentRequestDto;
import com.mvc.nms_be.dto.request.ReportCommentRequestDto;

import java.util.List;

public interface CommentService {

    /** Cây bình luận đa cấp của một bài viết (chỉ bình luận đang hiển thị). */
    List<CommentNodeDto> getArticleComments(Integer articleId);


    /** Đăng bình luận mới hoặc trả lời bình luận khác (AC-EN-001, AC-EN-002). */
    CommentResponseDto postComment(CreateCommentRequestDto request);

    /** Độc giả báo cáo bình luận vi phạm (AC-EN-003). */
    void reportComment(Integer commentId, ReportCommentRequestDto request);
}