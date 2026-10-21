package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.CategoryRequestDto;
import com.mvc.nms_be.dto.response.CategoryResponseDto;
import java.util.List;

// Các nghiệp vụ CRUD cơ bản của danh mục; controller chỉ gọi qua interface này.
public interface CategoryService {

    List<CategoryResponseDto> findAll();

    CategoryResponseDto getById(Integer id);

    CategoryResponseDto create(CategoryRequestDto request);

    CategoryResponseDto update(Integer id, CategoryRequestDto request);

    void delete(Integer id);
}
