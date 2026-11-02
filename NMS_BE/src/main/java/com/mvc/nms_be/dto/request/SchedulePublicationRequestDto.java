package com.mvc.nms_be.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record SchedulePublicationRequestDto(
        @NotNull(message = "Thời gian hẹn giờ xuất bản không được để trống")
        LocalDateTime scheduledPublishDate
) {}

