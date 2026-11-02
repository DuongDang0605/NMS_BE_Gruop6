package com.mvc.nms_be.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Độc giả báo cáo một bình luận vi phạm.
 * reason: SPAM, HARASSMENT, OFFENSIVE, MISINFORMATION, OTHER.
 * note: mô tả thêm (tùy chọn), service có thể ghép vào cột Reason (tối đa 500 ký tự).
 */
public record ReportCommentRequestDto(
        @NotBlank(message = "Lý do báo cáo không được để trống")
        @Pattern(regexp = "SPAM|HARASSMENT|OFFENSIVE|MISINFORMATION|OTHER",
                message = "Lý do báo cáo không hợp lệ")
        String reason,

        @Size(max = 400, message = "Ghi chú tối đa 400 ký tự")
        String note
) {
}