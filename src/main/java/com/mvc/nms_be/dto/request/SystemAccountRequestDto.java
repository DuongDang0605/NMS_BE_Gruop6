package com.mvc.nms_be.dto.request;

import com.mvc.nms_be.validation.Utf8Length;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SystemAccountRequestDto {

    @NotBlank(message = "Account name must not be blank")
    @Size(max = 100, message = "Account name must contain at most 100 characters")
    private String accountName;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be a valid email address")
    @Size(max = 254, message = "Email must contain at most 254 characters")
    private String accountEmail;

    @NotNull(message = "Account role is required")
    @Min(value = 1, message = "Account role must be 1 (Admin) or 2 (Staff)")
    @Max(value = 2, message = "Account role must be 1 (Admin) or 2 (Staff)")
    private Integer accountRole = 2;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 6, max = 72, message = "Password must contain between 6 and 72 characters")
    @Utf8Length
    private String accountPassword;
}