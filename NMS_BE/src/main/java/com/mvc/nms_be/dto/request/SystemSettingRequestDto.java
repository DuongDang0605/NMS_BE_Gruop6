package com.mvc.nms_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Setting key must not be blank")
    @Size(max = 100, message = "Setting key must contain at most 100 characters")
    private String settingKey;

    @NotNull(message = "Setting value is required")
    @Size(max = 1000, message = "Setting value must contain at most 1000 characters")
    private String settingValue;

    @Size(max = 500, message = "Description must contain at most 500 characters")
    private String description;
}
