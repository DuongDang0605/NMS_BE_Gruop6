package com.mvc.nms_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Dữ liệu client gửi lên khi tạo/cập nhật cấu hình; không chứa ID vì ID do database sinh.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingRequestDto {

    private String settingKey;
    private String settingValue;
    private String description;
}
