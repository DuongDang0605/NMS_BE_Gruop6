package com.mvc.nms_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDto {

    private Integer recipientId;
    private Integer newsArticleId;
    private String title;
    private String content;
    private Integer isRead = 0;
}
