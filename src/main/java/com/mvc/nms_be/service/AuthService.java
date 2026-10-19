package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.LoginRequestDto;
import com.mvc.nms_be.dto.response.LoginResponseDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface AuthService {
    LoginResponseDto login(@NotNull @Valid LoginRequestDto request);
    SystemAccountResponseDto me();
}
