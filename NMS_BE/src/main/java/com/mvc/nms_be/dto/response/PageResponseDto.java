package com.mvc.nms_be.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponseDto<T>(List<T> content, long totalElements, int totalPages,
                                 int page, int size, boolean first, boolean last) {
    public static <T> PageResponseDto<T> from(Page<T> result) {
        return new PageResponseDto<>(result.getContent(), result.getTotalElements(),
                result.getTotalPages(), result.getNumber(), result.getSize(),
                result.isFirst(), result.isLast());
    }
}
