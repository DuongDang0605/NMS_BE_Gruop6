package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.request.CategoryRequestDto;
import com.mvc.nms_be.dto.response.CategoryResponseDto;
import com.mvc.nms_be.service.CategoryService;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controller chỉ nhận request HTTP và gọi service; nghiệp vụ nằm trong CategoryServiceImpl.
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // GET /api/categories -> danh sách danh mục (200)
    @GetMapping
    public List<CategoryResponseDto> findAll() {
        return categoryService.findAll();
    }

    // GET /api/categories/{id} -> một danh mục (200) hoặc 404
    @GetMapping("/{id}")
    public CategoryResponseDto getById(@PathVariable Integer id) {
        return categoryService.getById(id);
    }

    // POST /api/categories -> tạo mới, trả 201 Created kèm header Location
    @PostMapping
    public ResponseEntity<CategoryResponseDto> create(@RequestBody CategoryRequestDto request) {
        CategoryResponseDto result = categoryService.create(request);
        return ResponseEntity.created(URI.create("/api/categories/" + result.getCategoryId())).body(result);
    }

    // PUT /api/categories/{id} -> cập nhật toàn bộ thông tin (200)
    @PutMapping("/{id}")
    public CategoryResponseDto update(@PathVariable Integer id, @RequestBody CategoryRequestDto request) {
        return categoryService.update(id, request);
    }

    // DELETE /api/categories/{id} -> 204 No Content; 409 nếu còn danh mục con hoặc bài viết
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
