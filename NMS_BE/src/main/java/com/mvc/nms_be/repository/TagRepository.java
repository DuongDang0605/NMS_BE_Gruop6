package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {

    List<Tag> findByTagNameContainingIgnoreCase(String keyword);

    Optional<Tag> findByTagNameIgnoreCase(String tagName);

    boolean existsByTagNameIgnoreCase(String tagName);

    boolean existsByTagNameIgnoreCaseAndTagIdNot(String tagName, Integer tagId);

    @Query("SELECT t FROM Tag t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.note) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Tag> searchTags(@Param("keyword") String keyword);
}
