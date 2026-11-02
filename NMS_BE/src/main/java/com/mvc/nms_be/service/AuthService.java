package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.LoginRequestDto;
import com.mvc.nms_be.dto.request.RefreshTokenRequestDto;
import com.mvc.nms_be.dto.request.RegisterReaderRequestDto;
import com.mvc.nms_be.dto.response.TokenRefreshResponseDto;
import com.mvc.nms_be.dto.response.LoginResponseDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface AuthService {
    SystemAccountResponseDto registerReader(@NotNull @Valid RegisterReaderRequestDto request);
    LoginResponseDto login(@NotNull @Valid LoginRequestDto request);
    TokenRefreshResponseDto refreshToken(@NotNull @Valid RefreshTokenRequestDto request);
    SystemAccountResponseDto me();
}
