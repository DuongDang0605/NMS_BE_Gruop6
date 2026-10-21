package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.NotificationRequestDto;
import com.mvc.nms_be.dto.response.NotificationResponseDto;
import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.entity.Notification;
import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.mapper.NotificationMapper;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.repository.NotificationRepository;
import com.mvc.nms_be.repository.SystemAccountRepository;
import com.mvc.nms_be.service.NotificationService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final SystemAccountRepository accountRepository;
    private final NewsArticleRepository articleRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public Page<NotificationResponseDto> findAll(
            Integer recipientId, String status, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw badRequest("page must be >= 0 and size must be between 1 and 100");
        }

        String filter = status == null ? "all" : status.trim().toLowerCase(Locale.ROOT);
        Integer isRead = switch (filter) {
            case "all" -> null;
            case "read" -> 1;
            case "unread" -> 0;
            default -> throw badRequest("status must be all, read, or unread");
        };
        requireRecipientExists(recipientId);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("notificationId")));
        Page<Notification> notifications = isRead == null
                ? notificationRepository.findByRecipient_AccountId(recipientId, pageable)
                : notificationRepository.findByRecipient_AccountIdAndIsRead(recipientId, isRead, pageable);
        return notifications.map(notificationMapper::toResponseDto);
    }

    @Override
    public NotificationResponseDto getById(Integer notificationId, Integer recipientId) {
        return notificationMapper.toResponseDto(findNotification(notificationId, recipientId));
    }

    @Override
    @Transactional
    public NotificationResponseDto create(NotificationRequestDto request) {
        if (request == null) {
            throw badRequest("Request body is required");
        }
        requirePositive(request.getRecipientId(), "recipientId");
        if (request.getNewsArticleId() != null) {
            requirePositive(request.getNewsArticleId(), "newsArticleId");
        }
        requireText(request.getTitle(), "title");
        requireText(request.getContent(), "content");
        if (request.getTitle().strip().length() > 250) {
            throw badRequest("title must contain at most 250 characters");
        }
        if (request.getIsRead() == null || request.getIsRead() != 0) {
            throw badRequest("isRead must be 0 when creating a notification");
        }
        request.setTitle(request.getTitle().strip());

        SystemAccount recipient = accountRepository.findById(request.getRecipientId())
                .orElseThrow(() -> notFound("Recipient not found"));
        NewsArticle article = null;
        if (request.getNewsArticleId() != null) {
            article = articleRepository.findById(request.getNewsArticleId())
                    .orElseThrow(() -> notFound("News article not found"));
        }

        Notification notification = notificationMapper.toEntity(request, recipient, article);
        return notificationMapper.toResponseDto(notificationRepository.saveAndFlush(notification));
    }

    @Override
    @Transactional
    public NotificationResponseDto markAsRead(Integer notificationId, Integer recipientId) {
        Notification notification = findNotification(notificationId, recipientId);
        if (notification.getIsRead() != 1) {
            notification.setIsRead(1);
        }
        return notificationMapper.toResponseDto(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(Integer recipientId) {
        requireRecipientExists(recipientId);
        return notificationRepository.markAllAsRead(recipientId);
    }

    @Override
    public long countUnread(Integer recipientId) {
        requireRecipientExists(recipientId);
        return notificationRepository.countByRecipient_AccountIdAndIsRead(recipientId, 0);
    }

    @Override
    @Transactional
    public void delete(Integer notificationId, Integer recipientId) {
        Notification notification = findNotification(notificationId, recipientId);
        notificationRepository.delete(notification);
    }

    private Notification findNotification(Integer notificationId, Integer recipientId) {
        requirePositive(notificationId, "notificationId");
        requirePositive(recipientId, "recipientId");
        return notificationRepository.findByNotificationIdAndRecipient_AccountId(notificationId, recipientId)
                .orElseThrow(() -> notFound("Notification not found"));
    }

    private void requireRecipientExists(Integer recipientId) {
        requirePositive(recipientId, "recipientId");
        if (!accountRepository.existsById(recipientId)) {
            throw notFound("Recipient not found");
        }
    }

    private static void requirePositive(Integer value, String name) {
        if (value == null || value <= 0) {
            throw badRequest(name + " must be a positive integer");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw badRequest(name + " must not be blank");
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
