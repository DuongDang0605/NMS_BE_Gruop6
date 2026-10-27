package com.mvc.nms_be.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSearchRequestDto {
    // Validate raw parameters before Pageable normalizes invalid values.
    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 10;

    @Size(max = 254)
    private String keyword;

    @Positive
    private Integer accountId;

    @Size(max = 100)
    private String name;

    @Size(max = 254)
    private String email;

    @Min(1)
    @Max(2)
    private Integer role;

}
