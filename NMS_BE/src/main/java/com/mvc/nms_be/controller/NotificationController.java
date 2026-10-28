package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.request.NotificationRequestDto;
import com.mvc.nms_be.dto.response.NotificationResponseDto;
import com.mvc.nms_be.service.NotificationService;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Page<NotificationResponseDto> findAll(
            @RequestParam Integer recipientId,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return notificationService.findAll(recipientId, status, page, size);
    }

    @GetMapping("/{id}")
    public NotificationResponseDto getById(@PathVariable Integer id, @RequestParam Integer recipientId) {
        return notificationService.getById(id, recipientId);
    }

    @PostMapping
    public ResponseEntity<NotificationResponseDto> create(
            @RequestBody NotificationRequestDto request) {
        NotificationResponseDto result = notificationService.create(request);
        URI location = URI.create("/api/v1/notifications/" + result.getNotificationId()
                + "?recipientId=" + result.getRecipientId());
        return ResponseEntity.created(location).body(result);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponseDto markAsRead(@PathVariable Integer id, @RequestParam Integer recipientId) {
        return notificationService.markAsRead(id, recipientId);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllAsRead(@RequestParam Integer recipientId) {
        return Map.of("updatedCount", notificationService.markAllAsRead(recipientId));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@RequestParam Integer recipientId) {
        return Map.of("unreadCount", notificationService.countUnread(recipientId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, @RequestParam Integer recipientId) {
        notificationService.delete(id, recipientId);
        return ResponseEntity.noContent().build();
    }
}
