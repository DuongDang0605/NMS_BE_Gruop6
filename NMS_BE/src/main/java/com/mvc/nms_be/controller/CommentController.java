package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.response.CommentNodeDto;
import com.mvc.nms_be.dto.response.CommentResponseDto;
import com.mvc.nms_be.dto.request.CreateCommentRequestDto;
import com.mvc.nms_be.dto.request.ReportCommentRequestDto;
import com.mvc.nms_be.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** Công khai: cây bình luận của một bài viết. */
    @GetMapping("/article/{articleId}")
    public ResponseEntity<List<CommentNodeDto>> getArticleComments(@PathVariable Integer articleId) {
        return ResponseEntity.ok(commentService.getArticleComments(articleId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponseDto> postComment(@Valid @RequestBody CreateCommentRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.postComment(request));
    }

    @PostMapping("/{commentId}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> reportComment(@PathVariable Integer commentId,
                                              @Valid @RequestBody ReportCommentRequestDto request) {
        commentService.reportComment(commentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}