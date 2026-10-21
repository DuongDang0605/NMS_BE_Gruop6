package com.mvc.nms_be.dto.response;

public record TokenRefreshResponseDto(String accessToken, String refreshToken, String tokenType, long expiresIn) { }
