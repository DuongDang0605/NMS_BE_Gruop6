package com.mvc.nms_be.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

public final class PasswordSupport {
    private static final Pattern BCRYPT = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");

    private PasswordSupport() { }

    public static boolean isBcryptHash(String value) {
        return value != null && BCRYPT.matcher(value).matches();
    }

    public static boolean matchesLegacyPassword(String rawPassword, String storedPassword) {
        return storedPassword != null && !isBcryptHash(storedPassword)
                && !storedPassword.startsWith("$2")
                && MessageDigest.isEqual(rawPassword.getBytes(StandardCharsets.UTF_8),
                        storedPassword.getBytes(StandardCharsets.UTF_8));
    }
}
