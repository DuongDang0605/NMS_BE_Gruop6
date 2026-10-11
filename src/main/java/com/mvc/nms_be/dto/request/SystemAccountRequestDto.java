package com.mvc.nms_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemAccountRequestDto {

    private String accountName;
    private String accountEmail;
    private Integer accountRole = 2;
    private String accountPassword;
}
