package com.mvc.nms_be.security;

import com.mvc.nms_be.entity.SystemAccount;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("recipientAuthorization")
public class RecipientAuthorization {

    // Use the same converted argument as the controller, never a second parser for the raw query string.
    public boolean canAccess(Integer recipientId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            return false;
        }
        return user.getAccountRole() == SystemAccount.ADMIN_ROLE
                || (recipientId != null && recipientId.equals(user.getAccountId()));
    }
}
