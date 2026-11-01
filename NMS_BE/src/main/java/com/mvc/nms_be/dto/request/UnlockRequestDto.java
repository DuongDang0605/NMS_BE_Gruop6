package com.mvc.nms_be.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnlockRequestDto(
        @NotBlank @Email @Size(max = 254) String accountEmail) {
}
