package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.BannedKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannedKeywordRepository extends JpaRepository<BannedKeyword, Integer> {

    /** Nạp vào cache của ProfanityFilterService. */
    List<BannedKeyword> findByIsActiveTrue();

    boolean existsByWordIgnoreCase(String word);

    boolean existsByWordIgnoreCaseAndKeywordIdNot(String word, Integer keywordId);
}