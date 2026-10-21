package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.CategoryRequestDto;
import com.mvc.nms_be.dto.response.CategoryResponseDto;
import com.mvc.nms_be.entity.Category;
import com.mvc.nms_be.mapper.CategoryMapper;
import com.mvc.nms_be.repository.CategoryRepository;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoryServiceImpl implements CategoryService {
}
