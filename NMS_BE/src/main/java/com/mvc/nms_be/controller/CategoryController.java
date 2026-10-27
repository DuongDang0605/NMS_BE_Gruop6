package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.response.CategoryTreeDto;
import com.mvc.nms_be.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * GET /api/v1/categories/tree
     * Trả về cây danh mục phân cấp để FE render menu navbar.
     * Public, không cần đăng nhập.
     */
    @GetMapping("/tree")
    public List<CategoryTreeDto> tree() {
        return categoryService.getTree();
    }
}
