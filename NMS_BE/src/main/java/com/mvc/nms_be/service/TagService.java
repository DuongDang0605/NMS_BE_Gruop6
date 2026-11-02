package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.TagRequestDto;
import com.mvc.nms_be.dto.response.TagResponseDto;
import java.util.List;

public interface TagService {

    List<TagResponseDto> getAllTags();

    TagResponseDto getTagById(Integer id);

    TagResponseDto createTag(TagRequestDto request);

    TagResponseDto updateTag(Integer id, TagRequestDto request);

    void deleteTag(Integer id);

    List<TagResponseDto> searchTags(String keyword);
}
