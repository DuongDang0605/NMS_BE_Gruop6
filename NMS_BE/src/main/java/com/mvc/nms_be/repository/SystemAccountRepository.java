package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.SystemAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemAccountRepository extends JpaRepository<SystemAccount, Integer>,
        JpaSpecificationExecutor<SystemAccount> {

    Optional<SystemAccount> findByAccountEmailIgnoreCase(String accountEmail);

    // Serialize login attempts for the same account, including across server instances.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SystemAccount a where lower(a.accountEmail) = lower(:email)")
    Optional<SystemAccount> findForLoginByEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SystemAccount a where a.accountId = :id")
    Optional<SystemAccount> findForUpdateById(@Param("id") Integer id);

    boolean existsByAccountEmailIgnoreCase(String accountEmail);

    boolean existsByAccountEmailIgnoreCaseAndAccountIdNot(String accountEmail, Integer accountId);

    long countByAccountRole(Integer accountRole);
}
