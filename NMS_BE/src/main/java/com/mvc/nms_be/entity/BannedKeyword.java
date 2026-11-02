package com.mvc.nms_be.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "BannedKeyword", uniqueConstraints =
@UniqueConstraint(name = "UQ_BannedKeyword_Word", columnNames = "Word"))
@Getter
@Setter
@NoArgsConstructor
public class BannedKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KeywordID")
    private Integer keywordId;

    @Column(name = "Word", nullable = false, length = 100)
    private String word;

    @Column(name = "Severity", nullable = false, length = 20)
    private String severity = "BLOCK"; // BLOCK: Chặn không cho gửi; FLAG: Cho gửi nhưng đưa vào PENDING_MODERATION

    @Column(name = "IsActive", nullable = false)
    private Boolean isActive = true;

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();
}