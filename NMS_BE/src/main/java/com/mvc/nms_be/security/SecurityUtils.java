package com.mvc.nms_be.security;

import com.mvc.nms_be.entity.NewsArticle;
import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.exception.UnauthorizedException;
import com.mvc.nms_be.exception.ForbiddenException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() { }

    public static CustomUserDetails getCurrentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new UnauthorizedException("Authentication is required");
        }
        return user;
    }

    public static SystemAccount getCurrentUser() {
        return getCurrentPrincipal().getAccount();
    }

    public static void requireAdmin() {
        if (!Objects.equals(getCurrentUser().getAccountRole(), SystemAccount.ADMIN_ROLE)) {
            throw new ForbiddenException("Only an admin can perform this operation");
        }
    }

    public static void requireArticleOwnerOrAdmin(NewsArticle article) {
        SystemAccount currentUser = getCurrentUser();
        if (currentUser.getAccountRole() != SystemAccount.ADMIN_ROLE
                && (article.getCreatedBy() == null
                || !Objects.equals(currentUser.getAccountId(), article.getCreatedBy().getAccountId()))) {
            throw new AccessDeniedException("Only the creator or an admin can change this article");
        }
    }

    public static void applyCreator(NewsArticle article) {
        article.setCreatedBy(getCurrentUser());
    }

    public static void applyUpdater(NewsArticle article) {
        requireArticleOwnerOrAdmin(article);
        article.setUpdatedBy(getCurrentUser());
        article.setModifiedDate(LocalDateTime.now(ZoneOffset.UTC));
    }
}
