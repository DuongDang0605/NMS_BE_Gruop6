package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.request.BannedKeywordRequestDto;
import com.mvc.nms_be.dto.response.BannedKeywordResponseDto;
import com.mvc.nms_be.dto.response.CommentReportResponseDto;
import com.mvc.nms_be.dto.response.CommentResponseDto;
import com.mvc.nms_be.dto.request.ModerationSettingRequestDto;
import com.mvc.nms_be.dto.response.ModerationSettingResponseDto;
import com.mvc.nms_be.dto.response.PageResponseDto;
import com.mvc.nms_be.service.ModerationService;
import com.mvc.nms_be.service.ModerationSettingService;
import com.mvc.nms_be.service.ProfanityFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/moderation")
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;
    private final ProfanityFilterService profanityFilterService;
    private final ModerationSettingService moderationSettingService;

    // ------------------------------------------------------------------ Hàng đợi bình luận

    @GetMapping("/comments/pending")
    public ResponseEntity<PageResponseDto<CommentResponseDto>> getPendingComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponseDto.from(
                moderationService.getPendingComments(pageOf(page, size))));
    }

    @GetMapping("/reports")
    public ResponseEntity<PageResponseDto<CommentReportResponseDto>> getReportList(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponseDto.from(
                moderationService.getReportList(status, pageOf(page, size))));
    }

    @PatchMapping("/comments/{commentId}/approve")
    public ResponseEntity<Void> approveComment(@PathVariable Integer commentId) {
        moderationService.approveComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/comments/{commentId}/reject")
    public ResponseEntity<Void> rejectComment(@PathVariable Integer commentId) {
        moderationService.rejectComment(commentId);
        return ResponseEntity.noContent().build();
    }

    /** "Giữ lại": bác bỏ báo cáo, giữ nguyên bình luận. */
    @PatchMapping("/comments/{commentId}/keep")
    public ResponseEntity<Void> keepComment(@PathVariable Integer commentId) {
        moderationService.keepComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Integer commentId) {
        moderationService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ Từ điển từ cấm

    @GetMapping("/keywords")
    public ResponseEntity<List<BannedKeywordResponseDto>> getKeywords() {
        return ResponseEntity.ok(profanityFilterService.getAllKeywords());
    }

    @PostMapping("/keywords")
    public ResponseEntity<BannedKeywordResponseDto> createKeyword(
            @Valid @RequestBody BannedKeywordRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profanityFilterService.createKeyword(request));
    }

    @PutMapping("/keywords/{keywordId}")
    public ResponseEntity<BannedKeywordResponseDto> updateKeyword(
            @PathVariable Integer keywordId,
            @Valid @RequestBody BannedKeywordRequestDto request) {
        return ResponseEntity.ok(profanityFilterService.updateKeyword(keywordId, request));
    }

    @DeleteMapping("/keywords/{keywordId}")
    public ResponseEntity<Void> deleteKeyword(@PathVariable Integer keywordId) {
        profanityFilterService.deleteKeyword(keywordId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ Chế độ kiểm duyệt toàn trang

    @GetMapping("/settings")
    public ResponseEntity<ModerationSettingResponseDto> getSettings() {
        return ResponseEntity.ok(
                new ModerationSettingResponseDto(moderationSettingService.isGlobalPreModeration()));
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModerationSettingResponseDto> updateSettings(
            @RequestBody ModerationSettingRequestDto request) {
        moderationSettingService.setGlobalPreModeration(request.preModeration());
        return ResponseEntity.ok(new ModerationSettingResponseDto(request.preModeration()));
    }

    /** Không nhận sort từ client vì các câu truy vấn đã có ORDER BY riêng. */
    private Pageable pageOf(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }
}