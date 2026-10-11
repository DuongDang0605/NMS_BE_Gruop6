package com.mvc.nms_be.mapper;

import com.mvc.nms_be.dto.request.TagRequestDto;
import com.mvc.nms_be.dto.response.TagResponseDto;
import com.mvc.nms_be.entity.Tag;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {

    public Tag toEntity(TagRequestDto request) {
        Tag tag = new Tag();
        tag.setTagName(request.getTagName());
        tag.setNote(request.getNote());
        return tag;
    }

    public TagResponseDto toResponseDto(Tag tag) {
        return new TagResponseDto(tag.getTagId(), tag.getTagName(), tag.getNote());
    }
}
