package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.TagRequestDto;
import com.mvc.nms_be.dto.response.TagResponseDto;
import com.mvc.nms_be.entity.Tag;
import com.mvc.nms_be.mapper.TagMapper;
import com.mvc.nms_be.repository.NewsTagRepository;
import com.mvc.nms_be.repository.TagRepository;
import com.mvc.nms_be.service.TagService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final NewsTagRepository newsTagRepository;
    private final TagMapper tagMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TagResponseDto> getAllTags() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponseDto getTagById(Integer id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found with id: " + id));
        return tagMapper.toResponseDto(tag);
    }

    @Override
    @Transactional
    public TagResponseDto createTag(TagRequestDto request) {
        if (request.getTagName() == null || request.getTagName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TagName cannot be empty");
        }
        String trimmedName = request.getTagName().trim();
        if (tagRepository.existsByTagNameIgnoreCase(trimmedName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag with name '" + trimmedName + "' already exists");
        }
        Tag tag = tagMapper.toEntity(request);
        tag.setTagName(trimmedName);
        Tag saved = tagRepository.save(tag);
        return tagMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public TagResponseDto updateTag(Integer id, TagRequestDto request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found with id: " + id));

        if (request.getTagName() == null || request.getTagName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TagName cannot be empty");
        }
        String trimmedName = request.getTagName().trim();
        if (tagRepository.existsByTagNameIgnoreCaseAndTagIdNot(trimmedName, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Another tag with name '" + trimmedName + "' already exists");
        }

        tagMapper.updateEntity(tag, request);
        tag.setTagName(trimmedName);
        Tag updated = tagRepository.save(tag);
        return tagMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteTag(Integer id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found with id: " + id));
        // Remove associations in NewsTag before deleting Tag to prevent FK violation
        newsTagRepository.deleteByTagId(id);
        tagRepository.delete(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponseDto> searchTags(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTags();
        }
        return tagRepository.searchTags(keyword.trim()).stream()
                .map(tagMapper::toResponseDto)
                .toList();
    }
}
