package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.SystemSettingRequestDto;
import com.mvc.nms_be.dto.response.SystemSettingResponseDto;
import java.util.List;

// Nghiệp vụ CRUD cho cấu hình hệ thống.
public interface SystemSettingService {

    List<SystemSettingResponseDto> findAll();

    SystemSettingResponseDto getById(Integer id);

    SystemSettingResponseDto getByKey(String key);

    SystemSettingResponseDto create(SystemSettingRequestDto request);

    SystemSettingResponseDto update(Integer id, SystemSettingRequestDto request);

    void delete(Integer id);
}
