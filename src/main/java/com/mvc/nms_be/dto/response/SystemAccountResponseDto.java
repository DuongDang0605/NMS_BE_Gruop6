package com.mvc.nms_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemAccountResponseDto {

    private Integer accountId;
    private String accountName;
    private String accountEmail;
    private Integer accountRole;
}
