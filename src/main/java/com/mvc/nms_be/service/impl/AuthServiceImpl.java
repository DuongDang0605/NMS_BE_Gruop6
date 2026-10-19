package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.LoginRequestDto;
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
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final SystemAccountMapper accountMapper;

    @Override
    public LoginResponseDto login(LoginRequestDto request) {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.getAccountEmail(), request.getAccountPassword()));
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return new LoginResponseDto(tokenProvider.generateToken(user), "Bearer", tokenProvider.getExpiresInSeconds(),
                user.getAccountId(), user.getAccountName(), user.getAccountEmail(), user.getAccountRole(), user.getRoleName());
    }

    @Override
    public SystemAccountResponseDto me() {
        return accountMapper.toResponseDto(SecurityUtils.getCurrentUser());
    }
}
