package com.mvc.nms_be.service;

import com.mvc.nms_be.dto.request.UnlockRequestDto;
import com.mvc.nms_be.dto.request.UnlockVerifyRequestDto;
import com.mvc.nms_be.dto.request.UnlockResetRequestDto;
import com.mvc.nms_be.dto.response.UnlockVerificationResponseDto;
import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.exception.UnauthorizedException;
import com.mvc.nms_be.repository.SystemAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccountUnlockService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String INVALID_OTP = "OTP is invalid or expired. Request a new code if necessary.";
    private final SystemAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final UnlockEmailService emailService;

    @Transactional
    public void requestOtp(UnlockRequestDto request) {
        SystemAccount account = find(request.accountEmail());
        // Password recovery is available whether or not the account is locked.
        // Keep the same public response for unknown and recently requested accounts.
        if (account == null) return;
        LocalDateTime now = now();
        if (account.getUnlockOtpSentAt() != null && account.getUnlockOtpSentAt().plusSeconds(60).isAfter(now)) return;
        String otp = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        account.setUnlockOtpHash(encoder.encode(otp));
        account.setUnlockOtpExpiresAt(now.plusMinutes(5));
        account.setUnlockOtpSentAt(now);
        account.setUnlockOtpAttempts(0);
        account.setUnlockResetTokenHash(null);
        account.setUnlockResetExpiresAt(null);
        // Delivery failure rolls back the challenge, so a retry is possible immediately.
        emailService.sendOtp(account.getAccountEmail(), otp);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public UnlockVerificationResponseDto verifyOtp(UnlockVerifyRequestDto request) {
        SystemAccount account = find(request.accountEmail());
        if (account == null || account.getUnlockOtpHash() == null
                || account.getUnlockOtpExpiresAt() == null || !account.getUnlockOtpExpiresAt().isAfter(now())
                || account.getUnlockOtpAttempts() >= 5) {
            throw new UnauthorizedException(INVALID_OTP);
        }
        if (!encoder.matches(request.otp(), account.getUnlockOtpHash())) {
            account.setUnlockOtpAttempts(account.getUnlockOtpAttempts() + 1);
            if (account.getUnlockOtpAttempts() >= 5) clearOtp(account);
            throw new UnauthorizedException(INVALID_OTP);
        }
        clearOtp(account);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        account.setUnlockResetTokenHash(hash(token));
        account.setUnlockResetExpiresAt(now().plusMinutes(5));
        // Verification alone must not change the password or account lock state.
        return new UnlockVerificationResponseDto(token, 300);
    }

    @Transactional
    public void resetPassword(UnlockResetRequestDto request) {
        SystemAccount account = find(request.accountEmail());
        if (account == null || account.getUnlockResetTokenHash() == null
                || account.getUnlockResetExpiresAt() == null || !account.getUnlockResetExpiresAt().isAfter(now())
                || !MessageDigest.isEqual(hash(request.resetToken()).getBytes(StandardCharsets.US_ASCII),
                        account.getUnlockResetTokenHash().getBytes(StandardCharsets.US_ASCII))) {
            throw new UnauthorizedException("Password reset authorization is invalid or expired. Verify a new OTP.");
        }
        if (encoder.matches(request.newPassword(), account.getAccountPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must differ from the old password.");
        }
        account.setAccountPassword(encoder.encode(request.newPassword()));
        account.setFailedLoginAttempts(0);
        account.setAccountLocked(false);
        account.clearUnlockChallenge();
    }

    private SystemAccount find(String email) {
        return accounts.findForLoginByEmail(email.strip()).orElse(null);
    }

    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }

    private static void clearOtp(SystemAccount account) {
        account.setUnlockOtpHash(null);
        account.setUnlockOtpExpiresAt(null);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
