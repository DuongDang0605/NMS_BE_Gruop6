package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.SystemAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemAccountRepository extends JpaRepository<SystemAccount, Integer> {

    Optional<SystemAccount> findByAccountEmailIgnoreCase(String accountEmail);

    boolean existsByAccountEmailIgnoreCase(String accountEmail);

    boolean existsByAccountEmailIgnoreCaseAndAccountIdNot(String accountEmail, Integer accountId);

    long countByAccountRole(Integer accountRole);
}
