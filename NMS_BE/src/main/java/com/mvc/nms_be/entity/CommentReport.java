package com.mvc.nms_be.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "CommentReport", uniqueConstraints =
@UniqueConstraint(name = "UQ_CommentReport_Comment_Reporter", columnNames = {"CommentID", "ReporterID"}))
@Getter
@Setter
@NoArgsConstructor
public class CommentReport {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_DISMISSED = "DISMISSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReportID")
    private Integer reportId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CommentID", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ReporterID", nullable = false)
    private SystemAccount reporter;

    @Column(name = "Reason", nullable = false, length = 500)
    private String reason; // SPAM, HARASSMENT, OFFENSIVE, MISINFORMATION, OTHER

    @Column(name = "Status", nullable = false, length = 30)
    private String status = STATUS_PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ResolvedByID")
    private SystemAccount resolvedBy;

    @Column(name = "CreatedDate", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "ResolvedDate")
    private LocalDateTime resolvedDate;
}