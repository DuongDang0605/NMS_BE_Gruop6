package com.mvc.nms_be.repository;

import com.mvc.nms_be.entity.NewsTag;
import com.mvc.nms_be.entity.NewsTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsTagRepository extends JpaRepository<NewsTag, NewsTagId> {
}
