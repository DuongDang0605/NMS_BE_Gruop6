package com.mvc.nms_be.controller;

import com.mvc.nms_be.dto.request.LoginRequestDto;
import com.mvc.nms_be.dto.request.RefreshTokenRequestDto;
import com.mvc.nms_be.dto.request.RegisterReaderRequestDto;
import com.mvc.nms_be.dto.response.TokenRefreshResponseDto;
import com.mvc.nms_be.dto.response.LoginResponseDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import com.mvc.nms_be.service.AuthService;
import com.mvc.nms_be.service.AccountUnlockService;
import com.mvc.nms_be.dto.request.UnlockRequestDto;
import com.mvc.nms_be.dto.request.UnlockVerifyRequestDto;
import com.mvc.nms_be.dto.request.UnlockResetRequestDto;
import com.mvc.nms_be.dto.response.UnlockVerificationResponseDto;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import java.net.URI;
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
    private final AccountUnlockService accountUnlockService;

    @PostMapping("/register-reader")
    @SecurityRequirements
    @Operation(summary = "Register an unverified Reader account")
    public ResponseEntity<SystemAccountResponseDto> registerReader(@Valid @RequestBody RegisterReaderRequestDto request) {
        var result = authService.registerReader(request);
        return ResponseEntity.created(URI.create("/api/v1/users/me")).body(result);
    }

    @PostMapping("/unlock/request")
    @Operation(summary = "Email a one-time code to a locked account")
    @SecurityRequirements
    public Map<String, String> requestUnlock(@Valid @RequestBody UnlockRequestDto request) {
        accountUnlockService.requestOtp(request);
        return Map.of("message", "If this email belongs to a locked account, an OTP has been sent. Please wait 60 seconds before requesting another code.");
    }

    @PostMapping("/unlock/verify")
    @Operation(summary = "Verify the OTP and authorize a password change for five minutes")
    @SecurityRequirements
    public UnlockVerificationResponseDto verifyUnlock(@Valid @RequestBody UnlockVerifyRequestDto request) {
        return accountUnlockService.verifyOtp(request);
    }

    @PostMapping("/unlock/reset-password")
    @Operation(summary = "Set a new password and unlock the account after OTP verification")
    @SecurityRequirements
    public Map<String, String> resetAndUnlock(@Valid @RequestBody UnlockResetRequestDto request) {
        accountUnlockService.resetPassword(request);
        return Map.of("message", "Password changed and account unlocked. Sign in with your new password.");
    }

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
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the currently authenticated account")
    public SystemAccountResponseDto me() {
        return authService.me();
    }
}
