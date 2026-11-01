package com.mvc.nms_be.dto.request;

import com.mvc.nms_be.validation.Utf8Length;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterReaderRequestDto(
        @NotBlank @Size(max = 100) String accountName,
        @NotBlank @Email @Size(max = 254) String accountEmail,
        @NotBlank @Size(min = 6, max = 50, message = "Password must contain between 6 and 50 characters")
        @Utf8Length String accountPassword) {
}
