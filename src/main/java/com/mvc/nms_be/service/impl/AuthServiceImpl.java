package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.LoginRequestDto;
import com.mvc.nms_be.dto.request.RefreshTokenRequestDto;
import com.mvc.nms_be.dto.response.TokenRefreshResponseDto;
import com.mvc.nms_be.exception.UnauthorizedException;
import com.mvc.nms_be.security.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import com.mvc.nms_be.dto.response.LoginResponseDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import com.mvc.nms_be.mapper.SystemAccountMapper;
import com.mvc.nms_be.security.CustomUserDetails;
import com.mvc.nms_be.security.JwtTokenProvider;
import com.mvc.nms_be.security.SecurityUtils;
import com.mvc.nms_be.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final SystemAccountMapper accountMapper;
    private final CustomUserDetailsService users;

    @Override
    public LoginResponseDto login(LoginRequestDto request) {
        try {
            var authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.getAccountEmail(), request.getAccountPassword()));
            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            return new LoginResponseDto(tokenProvider.generateAccessToken(user), tokenProvider.generateRefreshToken(user),
                    "Bearer", tokenProvider.getExpiresInSeconds(),
                    user.getAccountId(), user.getAccountName(), user.getAccountEmail(), user.getAccountRole(), user.getRoleName());
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    @Override
    public TokenRefreshResponseDto refreshToken(RefreshTokenRequestDto request) {
        try {
            var claims = tokenProvider.parseRefreshToken(request.refreshToken());
            int id = Integer.parseInt(claims.getSubject());
            CustomUserDetails user = users.loadUserById(id);
            if (id <= 0 || !tokenProvider.matchesCurrentAccount(claims, user)) {
                throw new UnauthorizedException("Refresh token is invalid or expired");
            }
            // Keep the original refresh expiry: refreshing must not extend the seven-day session.
            return new TokenRefreshResponseDto(tokenProvider.generateAccessToken(user), request.refreshToken(),
                    "Bearer", tokenProvider.getExpiresInSeconds());
        } catch (JwtException | IllegalArgumentException | AuthenticationException ex) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }
    }

    @Override
    public SystemAccountResponseDto me() {
        return accountMapper.toResponseDto(SecurityUtils.getCurrentUser());
    }
}
