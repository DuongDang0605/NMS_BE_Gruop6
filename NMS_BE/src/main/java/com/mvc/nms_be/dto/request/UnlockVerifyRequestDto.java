package com.mvc.nms_be.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UnlockVerifyRequestDto(
        @NotBlank @Email @Size(max = 254) String accountEmail,
        @NotBlank @Pattern(regexp = "[0-9]{6}", message = "OTP must contain 6 digits") String otp) {
}
