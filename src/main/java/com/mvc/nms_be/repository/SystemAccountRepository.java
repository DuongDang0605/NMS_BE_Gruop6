package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.SystemAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemAccountRepository extends JpaRepository<SystemAccount, Integer> {

    long countByAccountRole(Integer accountRole);
}
