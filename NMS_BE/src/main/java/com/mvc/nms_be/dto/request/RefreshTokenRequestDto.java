package com.mvc.nms_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequestDto(@NotBlank @Size(max = 4096) String refreshToken) { }
