package com.mvc.nms_be.dto.request;

import com.mvc.nms_be.validation.Utf8Length;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequestDto {
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be a valid email address")
    @Size(max = 254)
    private String accountEmail;

    @NotBlank(message = "Password must not be blank")
    @Size(max = 72)
    @Utf8Length
    private String accountPassword;
}
