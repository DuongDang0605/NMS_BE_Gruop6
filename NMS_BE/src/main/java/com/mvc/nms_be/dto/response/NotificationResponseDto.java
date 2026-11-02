package com.mvc.nms_be.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private Integer notificationId;
    private Integer recipientId;
    private Integer newsArticleId;
    private String title;
    private String content;
    private Integer isRead;
    private LocalDateTime createdDate;
}
