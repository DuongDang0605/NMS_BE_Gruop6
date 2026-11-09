package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.response.ProfanityCheckResult;
import com.mvc.nms_be.dto.request.BannedKeywordRequestDto;
import com.mvc.nms_be.dto.response.BannedKeywordResponseDto;

import com.mvc.nms_be.entity.BannedKeyword;
import com.mvc.nms_be.exception.ResourceNotFoundException;
import com.mvc.nms_be.mapper.CommentMapper;
import com.mvc.nms_be.repository.BannedKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Bộ lọc từ cấm tự động (FR-EN-003, AC-EN-002) kèm CRUD từ điển từ cấm.
 * <p>
 * Từ khóa đang hoạt động được cache trong bộ nhớ. Các hàm ghi (create/update/delete)
 * cố ý KHÔNG đánh dấu @Transactional để cache chỉ được làm mới sau khi dữ liệu đã commit.
 * Vì vậy không gọi các hàm ghi này từ bên trong một transaction khác.
 */
@Service
@RequiredArgsConstructor
public class ProfanityFilterService {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final BannedKeywordRepository bannedKeywordRepository;
    private final CommentMapper commentMapper;

    private record CachedKeyword(String original, String normalized, String severity) {
    }

    private volatile List<CachedKeyword> cache;

    // ------------------------------------------------------------------ Kiểm tra nội dung
    public ProfanityCheckResult checkContent(String content) {
        String text = normalize(content);
        if (text.isBlank()) {
            return ProfanityCheckResult.clean();
        }

        boolean blocked = false;
        boolean flagged = false;
        Set<String> detected = new LinkedHashSet<>();

        // Dùng snapshot cache; CRUD từ khóa gọi refreshCache sau khi lưu thành công.
        for (CachedKeyword keyword : getCache()) {
            if (text.contains(keyword.normalized())) {
                detected.add(keyword.original());
                if ("BLOCK".equals(keyword.severity())) {
                    blocked = true;
                } else {
                    flagged = true;
                }
            }
        }
        return new ProfanityCheckResult(blocked, flagged, List.copyOf(detected));
    }
    // ------------------------------------------------------------------ CRUD từ cấm

    public List<BannedKeywordResponseDto> getAllKeywords() {
        return bannedKeywordRepository.findAll(Sort.by("word")).stream()
                .map(commentMapper::toKeywordDto)
                .toList();
    }

    public BannedKeywordResponseDto createKeyword(BannedKeywordRequestDto request) {
        String word = cleanWord(request.word());
        if (bannedKeywordRepository.existsByWordIgnoreCase(word)) {
            throw new IllegalArgumentException("Từ khóa đã tồn tại: " + word);
        }
        BannedKeyword keyword = new BannedKeyword();
        keyword.setWord(word);
        keyword.setSeverity(request.severity());
        keyword.setIsActive(request.isActive() == null || request.isActive());
        BannedKeyword saved = bannedKeywordRepository.save(keyword);
        refreshCache();
        return commentMapper.toKeywordDto(saved);
    }

    public BannedKeywordResponseDto updateKeyword(Integer keywordId, BannedKeywordRequestDto request) {
        BannedKeyword keyword = bannedKeywordRepository.findById(keywordId)
                .orElseThrow(() -> new ResourceNotFoundException("Từ khóa không tồn tại: " + keywordId));
        String word = cleanWord(request.word());
        if (bannedKeywordRepository.existsByWordIgnoreCaseAndKeywordIdNot(word, keywordId)) {
            throw new IllegalArgumentException("Từ khóa đã tồn tại: " + word);
        }
        keyword.setWord(word);
        keyword.setSeverity(request.severity());
        if (request.isActive() != null) {
            keyword.setIsActive(request.isActive());
        }
        BannedKeyword saved = bannedKeywordRepository.save(keyword);
        refreshCache();
        return commentMapper.toKeywordDto(saved);
    }

    public void deleteKeyword(Integer keywordId) {
        if (!bannedKeywordRepository.existsById(keywordId)) {
            throw new ResourceNotFoundException("Từ khóa không tồn tại: " + keywordId);
        }
        bannedKeywordRepository.deleteById(keywordId);
        refreshCache();
    }

    // ------------------------------------------------------------------ Cache & chuẩn hóa

    public synchronized void refreshCache() {
        cache = bannedKeywordRepository.findByIsActiveTrue().stream()
                .map(k -> new CachedKeyword(k.getWord(), normalize(k.getWord()).trim(), k.getSeverity()))
                .filter(k -> !k.normalized().isEmpty())
                .toList();
    }

    private List<CachedKeyword> getCache() {
        List<CachedKeyword> current = cache;
        if (current == null) {
            refreshCache();
            current = cache;
        }
        return current;
    }

    private String cleanWord(String raw) {
        String word = raw == null ? "" : raw.trim();
        if (normalize(word).isBlank()) {
            throw new IllegalArgumentException("Từ khóa phải chứa ít nhất một chữ cái hoặc chữ số");
        }
        return word;
    }

    /**
     * Chữ thường, bỏ dấu tiếng Việt (kể cả đ/Đ), thay mọi ký tự không phải chữ/số bằng một
     * khoảng trắng và bọc khoảng trắng hai đầu.
     */
    static String normalize(String text) {
        if (text == null) {
            return " ";
        }
        String n = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        n = DIACRITICS.matcher(n).replaceAll("").replace('đ', 'd');
        n = NON_ALNUM.matcher(n).replaceAll(" ").trim();
        return " " + n + " ";
    }
}