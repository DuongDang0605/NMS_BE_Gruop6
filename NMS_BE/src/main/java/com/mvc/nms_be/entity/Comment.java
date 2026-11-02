package com.mvc.nms_be.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Comment", indexes = {
        @Index(name = "IX_Comment_Article_Status", columnList = "NewsArticleID, Status"),
        @Index(name = "IX_Comment_Parent", columnList = "ParentCommentID"),
        @Index(name = "IX_Comment_Account", columnList = "AccountID")
})
@Getter
@Setter
@NoArgsConstructor
public class Comment {

    public static final String STATUS_PENDING_MODERATION = "PENDING_MODERATION";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_DELETED = "DELETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CommentID")
    private Integer commentId;

    @Column(name = "NewsArticleID", nullable = false)
    private Integer newsArticleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AccountID", nullable = false)
    private SystemAccount account;

    // Quan hệ phân cấp: ParentCommentID trỏ về chính CommentID cha
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ParentCommentID")
    private Comment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL)
    @OrderBy("createdDate ASC")
    private List<Comment> replies = new ArrayList<>();

    @Column(name = "Content", nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    @Column(name = "Status", nullable = false, length = 30)
    private String status = STATUS_APPROVED;

    @Column(name = "IsFlaggedProfanity", nullable = false)
    private Boolean isFlaggedProfanity = false;

    @Column(name = "ReportCount", nullable = false)
    private Integer reportCount = 0;

    @Column(name = "CreatedDate", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;
}