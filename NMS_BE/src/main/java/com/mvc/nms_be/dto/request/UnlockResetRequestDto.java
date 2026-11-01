package com.mvc.nms_be.dto.request;

import com.mvc.nms_be.validation.Utf8Length;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnlockResetRequestDto(
        @NotBlank @Email @Size(max = 254) String accountEmail,
        @NotBlank @Size(max = 100) String resetToken,
        @NotBlank @Size(min = 6, max = 72) @Utf8Length String newPassword) {
}
