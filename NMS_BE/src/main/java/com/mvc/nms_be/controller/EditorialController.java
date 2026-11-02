package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.request.FeedbackRequestDto;
import com.mvc.nms_be.dto.request.SchedulePublicationRequestDto;
import com.mvc.nms_be.dto.response.ArticleAuditLogResponseDto;
import com.mvc.nms_be.dto.response.NewsArticleResponseDto;
import com.mvc.nms_be.service.EditorialService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/editorial")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
public class EditorialController {

    private final EditorialService editorialService;

    @GetMapping("/queue")
    public ResponseEntity<Page<NewsArticleResponseDto>> getReviewQueue(
            @PageableDefault(size = 10, sort = "modifiedDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(editorialService.getReviewQueue(pageable));
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<NewsArticleResponseDto> getArticleDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(editorialService.getArticleDetail(id));
    }

    @PostMapping("/articles/{id}/request-changes")
    public ResponseEntity<Void> requestChanges(
            @PathVariable Integer id,
            @Valid @RequestBody FeedbackRequestDto request) {
        editorialService.requestChanges(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/articles/{id}/publish-now")
    public ResponseEntity<NewsArticleResponseDto> approveAndPublishNow(@PathVariable Integer id) {
        return ResponseEntity.ok(editorialService.approveAndPublishNow(id));
    }

    @PostMapping("/articles/{id}/schedule")
    public ResponseEntity<NewsArticleResponseDto> approveAndSchedule(
            @PathVariable Integer id,
            @Valid @RequestBody SchedulePublicationRequestDto request) {
        return ResponseEntity.ok(editorialService.approveAndSchedule(id, request));
    }

    @PostMapping("/articles/{id}/archive")
    public ResponseEntity<NewsArticleResponseDto> archiveArticle(@PathVariable Integer id) {
        return ResponseEntity.ok(editorialService.archiveArticle(id));
    }

    @PostMapping("/articles/{id}/restore")
    public ResponseEntity<NewsArticleResponseDto> restoreArticle(@PathVariable Integer id) {
        return ResponseEntity.ok(editorialService.restoreArticle(id));
    }

    @GetMapping("/articles/{id}/audit-trail")
    public ResponseEntity<List<ArticleAuditLogResponseDto>> getArticleAuditTrail(@PathVariable Integer id) {
        return ResponseEntity.ok(editorialService.getArticleAuditTrail(id));
    }
}

