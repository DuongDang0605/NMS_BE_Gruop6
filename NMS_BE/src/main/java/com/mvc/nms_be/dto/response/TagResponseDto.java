package com.mvc.nms_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TagResponseDto {

    private Integer tagId;
    private String tagName;
    private String note;
}
