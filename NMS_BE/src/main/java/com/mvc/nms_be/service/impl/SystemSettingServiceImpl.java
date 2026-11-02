package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.SystemSettingRequestDto;
import com.mvc.nms_be.dto.response.SystemSettingResponseDto;
import com.mvc.nms_be.entity.SystemSetting;
import com.mvc.nms_be.exception.ResourceConflictException;
import com.mvc.nms_be.exception.ResourceNotFoundException;
import com.mvc.nms_be.mapper.SystemSettingMapper;
import com.mvc.nms_be.repository.SystemSettingRepository;
import com.mvc.nms_be.service.SystemSettingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Lỗi dữ liệu đầu vào (400) do @Valid + annotation trên SystemSettingRequestDto xử lý trước khi vào service.
// Service chỉ ném exception của nhóm: ResourceNotFoundException (404), ResourceConflictException (409).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository settingRepository;
    private final SystemSettingMapper settingMapper;

    @Override
    public List<SystemSettingResponseDto> findAll() {
        return settingRepository.findAll().stream()
                .map(settingMapper::toResponseDto)
                .toList();
    }

    @Override
    public SystemSettingResponseDto getById(Integer id) {
        return settingMapper.toResponseDto(findSetting(id));
    }

    @Override
    public SystemSettingResponseDto getByKey(String key) {
        SystemSetting setting = settingRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting with key '" + key + "' was not found"));
        return settingMapper.toResponseDto(setting);
    }

    @Override
    @Transactional
    public SystemSettingResponseDto create(SystemSettingRequestDto request) {
        request.setSettingKey(request.getSettingKey().strip());
        // Key phải duy nhất, trùng thì 409
        if (settingRepository.existsBySettingKey(request.getSettingKey())) {
            throw duplicateKey(request.getSettingKey());
        }
        SystemSetting setting = settingMapper.toEntity(request);
        return settingMapper.toResponseDto(settingRepository.save(setting));
    }

    @Override
    @Transactional
    public SystemSettingResponseDto update(Integer id, SystemSettingRequestDto request) {
        SystemSetting setting = findSetting(id);
        String newKey = request.getSettingKey().strip();
        // Đổi sang key khác thì key mới không được trùng với cấu hình khác
        if (!newKey.equals(setting.getSettingKey()) && settingRepository.existsBySettingKey(newKey)) {
            throw duplicateKey(newKey);
        }
        setting.setSettingKey(newKey);
        setting.setSettingValue(request.getSettingValue());
        setting.setDescription(request.getDescription());
        return settingMapper.toResponseDto(setting);   // Hibernate tự UPDATE khi transaction kết thúc
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        settingRepository.delete(findSetting(id));
    }

    // ===== Hàm hỗ trợ =====

    // Tìm cấu hình theo ID, không có thì 404
    private SystemSetting findSetting(Integer id) {
        return settingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setting with id " + id + " was not found"));
    }

    // 409 kèm field "settingKey" (giống cách module User báo trùng email)
    private static ResourceConflictException duplicateKey(String key) {
        return new ResourceConflictException("Setting key '" + key + "' is already in use", "settingKey");
    }
}
