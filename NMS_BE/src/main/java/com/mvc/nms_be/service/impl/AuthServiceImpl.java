package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.LoginRequestDto;
import com.mvc.nms_be.dto.request.RefreshTokenRequestDto;
import com.mvc.nms_be.dto.request.RegisterReaderRequestDto;
import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.exception.ResourceConflictException;
import com.mvc.nms_be.dto.response.TokenRefreshResponseDto;
import com.mvc.nms_be.exception.UnauthorizedException;
import com.mvc.nms_be.exception.AccountLockedException;
import com.mvc.nms_be.exception.AccountInactiveException;
import com.mvc.nms_be.repository.SystemAccountRepository;
import com.mvc.nms_be.security.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import com.mvc.nms_be.dto.response.LoginResponseDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import com.mvc.nms_be.mapper.SystemAccountMapper;
import com.mvc.nms_be.security.CustomUserDetails;
import com.mvc.nms_be.security.JwtTokenProvider;
import com.mvc.nms_be.security.SecurityUtils;
import com.mvc.nms_be.security.PasswordSupport;
import com.mvc.nms_be.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final SystemAccountMapper accountMapper;
    private final CustomUserDetailsService users;
    private final SystemAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public SystemAccountResponseDto registerReader(RegisterReaderRequestDto request) {
        String email = request.accountEmail().strip().toLowerCase(Locale.ROOT);
        if (accountRepository.existsByAccountEmailIgnoreCase(email)) {
            throw new ResourceConflictException("Email is already in use", "accountEmail");
        }
        SystemAccount account = new SystemAccount();
        account.setAccountName(request.accountName().strip());
        account.setAccountEmail(email);
        account.setAccountPassword(passwordEncoder.encode(request.accountPassword()));
        account.setAccountRole(SystemAccount.READER_ROLE);
        account.setIsVerified(false);
        return accountMapper.toResponseDto(accountRepository.saveAndFlush(account));
    }

    @Override
    // Commit the counter/lock even though an unsuccessful login returns HTTP 401.
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public LoginResponseDto login(LoginRequestDto request) {
        var account = accountRepository.findForLoginByEmail(request.getAccountEmail().strip()).orElse(null);
        if (account != null && !account.isActive()) {
            throw new AccountInactiveException();
        }
        if (account != null && account.isAccountLocked()) {
            throw new AccountLockedException();
        }
        // Upgrade only after a correct legacy password; preserve failed-login counting and account locks.
        if (account != null && SystemAccount.isSupportedRole(account.getAccountRole())
                && PasswordSupport.matchesLegacyPassword(request.getAccountPassword(), account.getAccountPassword())) {
            account.setAccountPassword(passwordEncoder.encode(request.getAccountPassword()));
        }
        try {
            var authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.getAccountEmail(), request.getAccountPassword()));
            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            if (account != null) {
                account.setFailedLoginAttempts(0);
            }
            return new LoginResponseDto(tokenProvider.generateAccessToken(user), tokenProvider.generateRefreshToken(user),
                    "Bearer", tokenProvider.getExpiresInSeconds(),
                    user.getAccountId(), user.getAccountName(), user.getAccountEmail(), user.getAccountRole(), user.getRoleName(), user.isVerified());
        } catch (DisabledException ex) {
            throw new AccountInactiveException();
        } catch (LockedException ex) {
            throw new AccountLockedException();
        } catch (BadCredentialsException ex) {
            if (account != null) {
                account.setFailedLoginAttempts(account.getFailedLoginAttempts() + 1);
                if (account.getFailedLoginAttempts() > MAX_FAILED_ATTEMPTS) {
                    account.setAccountLocked(true);
                    throw new AccountLockedException();
                }
            }
            throw new UnauthorizedException("Invalid email or password");
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    @Override
    public TokenRefreshResponseDto refreshToken(RefreshTokenRequestDto request) {
        try {
            var claims = tokenProvider.parseRefreshToken(request.refreshToken());
            int id = tokenProvider.getAccountId(claims);
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
        return accountMapper.toResponseDto(SecurityUtils.getCurrentUser().getAccount());
    }
}
