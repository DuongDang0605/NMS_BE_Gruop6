package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.request.SystemSettingRequestDto;
import com.mvc.nms_be.dto.response.SystemSettingResponseDto;
import com.mvc.nms_be.service.SystemSettingService;
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

// API cấu hình hệ thống: /api/v1/settings (đọc: Admin/Staff, ghi: Admin - xem SecurityConfig)
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService settingService;

    // GET /api/v1/settings -> tất cả cấu hình
    @GetMapping
    public List<SystemSettingResponseDto> findAll() {
        return settingService.findAll();
    }

    // GET /api/v1/settings/{id} -> cấu hình theo ID
    @GetMapping("/{id}")
    public SystemSettingResponseDto getById(@PathVariable Integer id) {
        return settingService.getById(id);
    }

    // GET /api/v1/settings/key/{key} -> cấu hình theo key, ví dụ /api/v1/settings/key/SiteName
    @GetMapping("/key/{key}")
    public SystemSettingResponseDto getByKey(@PathVariable String key) {
        return settingService.getByKey(key);
    }

    // POST /api/v1/settings -> tạo mới (201 Created); key trùng trả 409
    @PostMapping
    public ResponseEntity<SystemSettingResponseDto> create(@RequestBody SystemSettingRequestDto request) {
        SystemSettingResponseDto result = settingService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/settings/" + result.getSettingId())).body(result);
    }

    // PUT /api/v1/settings/{id} -> cập nhật
    @PutMapping("/{id}")
    public SystemSettingResponseDto update(@PathVariable Integer id, @RequestBody SystemSettingRequestDto request) {
        return settingService.update(id, request);
    }

    // DELETE /api/v1/settings/{id} -> 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        settingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
