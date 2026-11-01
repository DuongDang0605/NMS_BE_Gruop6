package com.mvc.nms_be.dto.response;

public record UnlockVerificationResponseDto(String resetToken, long expiresIn) {
}
