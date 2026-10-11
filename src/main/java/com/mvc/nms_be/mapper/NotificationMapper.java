package com.mvc.nms_be.mapper;

import com.mvc.nms_be.dto.request.NotificationRequestDto;
import com.mvc.nms_be.dto.response.NotificationResponseDto;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.entity.Notification;
import com.mvc.nms_be.entity.SystemAccount;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    // The service resolves recipientId and optional newsArticleId from the request.
    public Notification toEntity(NotificationRequestDto request, SystemAccount recipient, NewsArticle article) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setNewsArticle(article);
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setIsRead(request.getIsRead());
        return notification;
    }

    public NotificationResponseDto toResponseDto(Notification notification) {
        return new NotificationResponseDto(
                notification.getNotificationId(),
                notification.getRecipient().getAccountId(),
                notification.getNewsArticle() == null ? null : notification.getNewsArticle().getNewsArticleId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getIsRead(),
                notification.getCreatedDate()
        );
    }
}
