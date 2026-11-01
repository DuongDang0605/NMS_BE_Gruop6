package com.mvc.nms_be.dto.request;

import com.mvc.nms_be.validation.Utf8Length;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(
        @NotBlank @Size(max = 72) @Utf8Length String currentPassword,
        @NotBlank @Size(min = 6, max = 72) @Utf8Length String newPassword) {
}
