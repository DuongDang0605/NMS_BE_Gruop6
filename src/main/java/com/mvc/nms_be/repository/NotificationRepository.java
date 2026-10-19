package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @EntityGraph(attributePaths = {"recipient", "newsArticle"})
    Optional<Notification> findByNotificationIdAndRecipient_AccountId(
            Integer notificationId, Integer recipientId);

    @EntityGraph(attributePaths = {"recipient", "newsArticle"})
    Page<Notification> findByRecipient_AccountId(Integer recipientId, Pageable pageable);

    @EntityGraph(attributePaths = {"recipient", "newsArticle"})
    Page<Notification> findByRecipient_AccountIdAndIsRead(
            Integer recipientId, Integer isRead, Pageable pageable);

    long countByRecipient_AccountIdAndIsRead(Integer recipientId, Integer isRead);

    long countByRecipient_AccountId(Integer recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = 1 "
            + "WHERE n.recipient.accountId = :recipientId AND n.isRead = 0")
    int markAllAsRead(@Param("recipientId") Integer recipientId);
}
