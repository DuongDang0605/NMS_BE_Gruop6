package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.request.LoginRequestDto;
import com.mvc.nms_be.dto.request.RefreshTokenRequestDto;
import com.mvc.nms_be.dto.response.TokenRefreshResponseDto;
import com.mvc.nms_be.dto.response.LoginResponseDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import com.mvc.nms_be.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with an account stored in the database")
    @SecurityRequirements
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Issue a new access token using a valid refresh token")
    @SecurityRequirements
    public TokenRefreshResponseDto refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        return authService.refreshToken(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated account")
    public SystemAccountResponseDto me() {
        return authService.me();
    }
}
