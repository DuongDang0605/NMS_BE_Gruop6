package com.mvc.nms_be.dto.response;

public record LoginResponseDto(String accessToken, String refreshToken, String tokenType, long expiresIn,
        Integer accountId, String accountName, String accountEmail, Integer accountRole, String roleName) {
}
