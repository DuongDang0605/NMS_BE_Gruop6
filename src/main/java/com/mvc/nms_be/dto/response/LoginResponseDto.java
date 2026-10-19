package com.mvc.nms_be.dto.response;

public record LoginResponseDto(String token, String tokenType, long expiresIn,
        Integer accountId, String accountName, String accountEmail, Integer accountRole, String roleName) {
}
