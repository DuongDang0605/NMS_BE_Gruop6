package com.mvc.nms_be.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvc.nms_be.entity.SystemAccount;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {
    private final SystemAccount account;

    public CustomUserDetails(SystemAccount account) {
        this.account = account;
    }

    public Integer getAccountId() { return account.getAccountId(); }
    public String getAccountName() { return account.getAccountName(); }
    public String getAccountEmail() { return account.getAccountEmail(); }
    public Integer getAccountRole() { return account.getAccountRole(); }

    public String getRoleName() {
        return switch (account.getAccountRole()) {
            case SystemAccount.ADMIN_ROLE -> "ADMIN";
            case SystemAccount.STAFF_ROLE -> "STAFF";
            default -> throw new IllegalStateException("Unsupported account role");
        };
    }

    @JsonIgnore
    public SystemAccount getAccount() { return account; }

    @Override
    @JsonIgnore
    public String getPassword() { return account.getAccountPassword(); }

    @Override
    public String getUsername() { return account.getAccountEmail(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + getRoleName()));
    }
}
