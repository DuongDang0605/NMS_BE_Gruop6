package com.mvc.nms_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Thêm hoặc cập nhật từ khóa cấm. severity: BLOCK hoặc FLAG.
 */
public record BannedKeywordRequestDto(
        @NotBlank(message = "Từ khóa không được để trống")
        @Size(max = 100, message = "Từ khóa tối đa 100 ký tự")
        String word,

        @NotBlank(message = "Mức độ không được để trống")
        @Pattern(regexp = "BLOCK|FLAG", message = "Mức độ phải là BLOCK hoặc FLAG")
        String severity,

        Boolean isActive
) {
}
