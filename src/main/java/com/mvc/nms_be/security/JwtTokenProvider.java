package com.mvc.nms_be.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;
    private final String issuer;

    public JwtTokenProvider(@Value("${jwt.secret:}") String secret,
                            @Value("${jwt.access-expiration-ms:1800000}") long expirationMs,
                            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs,
                            @Value("${jwt.issuer:NMS_BE}") String issuer) {
        if (expirationMs < 1000 || refreshExpirationMs < 1000 || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT expiration must be positive and issuer must not be blank");
        }
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.issuer = issuer;
        if (secret.isBlank()) {
            key = Jwts.SIG.HS256.key().build();
            log.warn("JWT_SECRET is unset; using a random local key. Tokens expire on restart.");
        } else {
            byte[] bytes = secret.matches("[0-9a-fA-F]{64,}") && secret.length() % 2 == 0
                    ? HexFormat.of().parseHex(secret) : Decoders.BASE64.decode(secret);
            key = Keys.hmacShaKeyFor(bytes);
        }
    }

    public String generateAccessToken(CustomUserDetails user) {
        return generate(user, "ACCESS", expirationMs);
    }

    public String generateRefreshToken(CustomUserDetails user) {
        return generate(user, "REFRESH", refreshExpirationMs);
    }

    private String generate(CustomUserDetails user, String type, long validityMs) {
        Instant issuedAt = Instant.now();
        return Jwts.builder().issuer(issuer).subject(user.getAccountId().toString())
                .id(UUID.randomUUID().toString())
                .claim("type", type)
                .claim("accountId", user.getAccountId())
                .claim("accountRole", user.getAccountRole())
                .claim("accountName", user.getAccountName())
                .claim("email", user.getAccountEmail())
                .claim("roles", user.getAuthorities().stream().map(a -> a.getAuthority()).toList())
                .claim("pv", passwordVersion(user.getPassword()))
                .issuedAt(Date.from(issuedAt)).expiration(Date.from(issuedAt.plusMillis(validityMs)))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    public Claims parseAccessToken(String token) {
        return parse(token, "ACCESS");
    }

    public Claims parseRefreshToken(String token) {
        return parse(token, "REFRESH");
    }

    private Claims parse(String token, String type) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer(issuer)
                .require("type", type)
                .build().parseSignedClaims(token).getPayload();
        if (claims.getSubject() == null || claims.getExpiration() == null || claims.getIssuedAt() == null) {
            throw new MalformedJwtException("Missing required token claims");
        }
        return claims;
    }

    public boolean matchesCurrentAccount(Claims claims, CustomUserDetails user) {
        // Reloaded database roles take effect immediately; password changes invalidate old tokens.
        return user.getAccountEmail().equals(claims.get("email", String.class))
                && passwordVersion(user.getPassword()).equals(claims.get("pv", String.class));
    }

    public long getExpiresInSeconds() { return expirationMs / 1000; }

    private static String passwordVersion(String hash) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(hash.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
