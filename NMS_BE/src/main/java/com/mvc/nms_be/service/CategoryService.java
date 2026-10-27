package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.CategoryRequestDto;
import com.mvc.nms_be.dto.response.CategoryResponseDto;
import com.mvc.nms_be.dto.response.CategoryTreeDto;
import java.util.List;

// Các nghiệp vụ CRUD cơ bản của danh mục; controller chỉ gọi qua interface này.
public interface CategoryService {

    /** Trả về cây danh mục phân cấp (root → children) để render menu navbar. */
    List<CategoryTreeDto> getTree();
}
