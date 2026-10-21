package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.NotificationRequestDto;
import com.mvc.nms_be.dto.response.NotificationResponseDto;
import org.springframework.data.domain.Page;

public interface NotificationService {

    Page<NotificationResponseDto> findAll(Integer recipientId, String status, int page, int size);

    NotificationResponseDto getById(Integer notificationId, Integer recipientId);

    NotificationResponseDto create(NotificationRequestDto request);

    NotificationResponseDto markAsRead(Integer notificationId, Integer recipientId);

    int markAllAsRead(Integer recipientId);

    long countUnread(Integer recipientId);

    void delete(Integer notificationId, Integer recipientId);
}
