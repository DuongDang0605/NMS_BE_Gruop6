package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.SystemSettingRequestDto;
import com.mvc.nms_be.dto.response.SystemSettingResponseDto;
import com.mvc.nms_be.entity.SystemSetting;
import com.mvc.nms_be.mapper.SystemSettingMapper;
import com.mvc.nms_be.repository.SystemSettingRepository;
import com.mvc.nms_be.service.SystemSettingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Setting not found"));
        return settingMapper.toResponseDto(setting);
    }

    @Override
    @Transactional
    public SystemSettingResponseDto create(SystemSettingRequestDto request) {
        validate(request);
        request.setSettingKey(request.getSettingKey().strip());
        // Key phải duy nhất -> trùng thì trả 409 Conflict
        if (settingRepository.existsBySettingKey(request.getSettingKey())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "settingKey already exists");
        }
        SystemSetting setting = settingMapper.toEntity(request);
        return settingMapper.toResponseDto(settingRepository.save(setting));
    }

    @Override
    @Transactional
    public SystemSettingResponseDto update(Integer id, SystemSettingRequestDto request) {
        validate(request);
        SystemSetting setting = findSetting(id);
        String newKey = request.getSettingKey().strip();
        // Đổi sang key khác thì key mới không được trùng với cấu hình khác
        if (!newKey.equals(setting.getSettingKey()) && settingRepository.existsBySettingKey(newKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "settingKey already exists");
        }
        setting.setSettingKey(newKey);
        setting.setSettingValue(request.getSettingValue());
        setting.setDescription(request.getDescription());
        return settingMapper.toResponseDto(setting);   // Hibernate tự UPDATE khi commit transaction
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        settingRepository.delete(findSetting(id));
    }

    // ===== Hàm hỗ trợ =====

    private SystemSetting findSetting(Integer id) {
        return settingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Setting not found"));
    }

    // Kiểm tra dữ liệu đầu vào cơ bản, sai thì trả 400 Bad Request.
    private static void validate(SystemSettingRequestDto request) {
        if (request == null || request.getSettingKey() == null || request.getSettingKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "settingKey must not be blank");
        }
        if (request.getSettingKey().strip().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "settingKey must contain at most 100 characters");
        }
        if (request.getSettingValue() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "settingValue is required");
        }
        if (request.getSettingValue().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "settingValue must contain at most 1000 characters");
        }
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description must contain at most 500 characters");
        }
    }
}
