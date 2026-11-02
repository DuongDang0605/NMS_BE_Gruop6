package com.mvc.nms_be;
 
import java.util.List;
 
/**
 * Kết quả quét từ cấm của ProfanityFilterService.
 */
public record ProfanityCheckResult(
        boolean hasBlockedWord,
        boolean hasFlaggedWord,
        List<String> detectedWords
) {
    public static ProfanityCheckResult clean() {
        return new ProfanityCheckResult(false, false, List.of());
    }
}
 
