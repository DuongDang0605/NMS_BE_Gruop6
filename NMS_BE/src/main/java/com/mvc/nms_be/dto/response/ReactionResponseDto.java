package com.mvc.nms_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionResponseDto {

    private Integer articleId;
    private Integer likeCount;
    private Boolean isLikedByCurrentUser;
}
