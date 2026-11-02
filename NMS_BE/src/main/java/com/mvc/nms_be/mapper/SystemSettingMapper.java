package com.mvc.nms_be.mapper;

import com.mvc.nms_be.dto.request.SystemSettingRequestDto;
import com.mvc.nms_be.dto.response.SystemSettingResponseDto;
import com.mvc.nms_be.entity.SystemSetting;
import org.springframework.stereotype.Component;

// Chuyển đổi qua lại giữa DTO và entity (viết tay, giống các mapper khác của nhóm).
@Component
public class SystemSettingMapper {

    public SystemSetting toEntity(SystemSettingRequestDto request) {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(request.getSettingKey());
        setting.setSettingValue(request.getSettingValue());
        setting.setDescription(request.getDescription());
        return setting;
    }

    public SystemSettingResponseDto toResponseDto(SystemSetting setting) {
        return new SystemSettingResponseDto(
                setting.getSettingId(),
                setting.getSettingKey(),
                setting.getSettingValue(),
                setting.getDescription()
        );
    }
}
