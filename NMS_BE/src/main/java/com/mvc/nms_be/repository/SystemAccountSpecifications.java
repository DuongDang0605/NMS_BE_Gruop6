package com.mvc.nms_be.repository;

import com.mvc.nms_be.dto.request.UserSearchRequestDto;
import com.mvc.nms_be.entity.SystemAccount;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class SystemAccountSpecifications {
    private SystemAccountSpecifications() {}

    public static Specification<SystemAccount> matching(UserSearchRequestDto filter) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (hasText(filter.getKeyword())) {
                String keyword = contains(filter.getKeyword());
                predicates.add(cb.or(cb.like(cb.lower(root.get("accountName")), keyword, '!'),
                        cb.like(cb.lower(root.get("accountEmail")), keyword, '!')));
            }
            if (filter.getAccountId() != null) {
                predicates.add(cb.equal(root.get("accountId"), filter.getAccountId()));
            }
            if (hasText(filter.getName())) {
                predicates.add(cb.like(cb.lower(root.get("accountName")), contains(filter.getName()), '!'));
            }
            if (hasText(filter.getEmail())) {
                predicates.add(cb.like(cb.lower(root.get("accountEmail")), contains(filter.getEmail()), '!'));
            }
            if (filter.getRole() != null) {
                predicates.add(cb.equal(root.get("accountRole"), filter.getRole()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // Escape LIKE wildcards, including SQL Server's bracket pattern syntax.
    private static String contains(String value) {
        return "%" + value.strip().toLowerCase(Locale.ROOT).replace("!", "!!")
                .replace("%", "!%").replace("_", "!_").replace("[", "![") + "%";
    }
}
