package com.mvc.nms_be.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FeedbackRequestDto(
        @NotBlank(message = "Nội dung phản hồi không được để trống")
        String feedbackContent
) {}

