package com.mvc.nms_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "SystemAccount", uniqueConstraints =
        @UniqueConstraint(name = "UQ_SystemAccount_Email", columnNames = "AccountEmail"))
@Check(name = "CK_SystemAccount_Role", constraints = "AccountRole IN (1, 2, 3, 4, 5)")
@Getter
@Setter
@NoArgsConstructor
public class SystemAccount {

    public static final int ADMIN_ROLE = 1;
    public static final int CONTRIBUTOR_ROLE = 2;
    public static final int EDITOR_ROLE = 3;
    public static final int MODERATOR_ROLE = 4;
    public static final int READER_ROLE = 5;
    // Compatibility for existing integrations; role 2 now grants CONTRIBUTOR authority.
    @Deprecated
    public static final int STAFF_ROLE = CONTRIBUTOR_ROLE;

    public static boolean isSupportedRole(Integer role) { return role != null && role >= 1 && role <= 5; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AccountID")
    private Integer accountId;

    @Column(name = "AccountName", nullable = false, length = 100, columnDefinition = "nvarchar(100)")
    private String accountName;

    @Column(name = "AccountEmail", nullable = false, length = 254)
    private String accountEmail;

    @Column(name = "AccountRole", nullable = false)
    private Integer accountRole = READER_ROLE;

    @Column(name = "IsVerified", nullable = false, columnDefinition = "bit default 0")
    private Boolean isVerified = false;

    // BCrypt hash; raw passwords are never returned by the API.
    @Column(name = "AccountPassword", nullable = false, length = 255)
    private String accountPassword;

    @Column(name = "FailedLoginAttempts", nullable = false, columnDefinition = "int default 0")
    private int failedLoginAttempts = 0;

    @Column(name = "AccountLocked", nullable = false, columnDefinition = "bit default 0")
    private boolean accountLocked = false;

    @Column(name = "UnlockOtpHash", length = 100)
    private String unlockOtpHash;

    @Column(name = "UnlockOtpExpiresAt")
    private LocalDateTime unlockOtpExpiresAt;

    @Column(name = "UnlockOtpSentAt")
    private LocalDateTime unlockOtpSentAt;

    @Column(name = "UnlockOtpAttempts", nullable = false, columnDefinition = "int default 0")
    private int unlockOtpAttempts = 0;

    @Column(name = "UnlockResetTokenHash", length = 64)
    private String unlockResetTokenHash;

    @Column(name = "UnlockResetExpiresAt")
    private LocalDateTime unlockResetExpiresAt;

    public void clearUnlockChallenge() {
        unlockOtpHash = null;
        unlockOtpExpiresAt = null;
        unlockOtpSentAt = null;
        unlockOtpAttempts = 0;
        unlockResetTokenHash = null;
        unlockResetExpiresAt = null;
    }
}
