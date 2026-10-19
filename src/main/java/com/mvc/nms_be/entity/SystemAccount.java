package com.mvc.nms_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "SystemAccount", uniqueConstraints =
        @UniqueConstraint(name = "UQ_SystemAccount_Email", columnNames = "AccountEmail"))
@Check(name = "CK_SystemAccount_Role", constraints = "AccountRole IN (1, 2)")
@Getter
@Setter
@NoArgsConstructor
public class SystemAccount {

    public static final int ADMIN_ROLE = 1;
    public static final int STAFF_ROLE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AccountID")
    private Integer accountId;

    @Column(name = "AccountName", nullable = false, length = 100)
    private String accountName;

    @Column(name = "AccountEmail", nullable = false, length = 254)
    private String accountEmail;

    @Column(name = "AccountRole", nullable = false)
    private Integer accountRole = STAFF_ROLE;

    // BCrypt hash; raw passwords are never returned by the API.
    @Column(name = "AccountPassword", nullable = false, length = 255)
    private String accountPassword;
}
