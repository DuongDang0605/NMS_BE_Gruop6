package com.mvc.nms_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Dữ liệu trả về cho client.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingResponseDto {

    private Integer settingId;
    private String settingKey;
    private String settingValue;
    private String description;
}
