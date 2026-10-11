package com.mvc.nms_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "Notification", indexes = {
        @Index(name = "IX_Notification_Recipient_Read_Date", columnList = "RecipientID, IsRead, CreatedDate"),
        @Index(name = "IX_Notification_Article", columnList = "NewsArticleID")
})
@Check(name = "CK_Notification_Read", constraints = "IsRead IN (0, 1)")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID")
    private Integer notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RecipientID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_Notification_Recipient"))
    private SystemAccount recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NewsArticleID", foreignKey = @ForeignKey(name = "FK_Notification_Article"))
    private NewsArticle newsArticle;

    @Column(name = "Title", nullable = false, length = 250, columnDefinition = "nvarchar(250)")
    private String title;

    @Column(name = "Content", nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    @Column(name = "IsRead", nullable = false)
    private Integer isRead = 0;

    @Column(name = "CreatedDate", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    private void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
