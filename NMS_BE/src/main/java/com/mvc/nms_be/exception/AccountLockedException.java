package com.mvc.nms_be.exception;

public class AccountLockedException extends UnauthorizedException {
    public AccountLockedException() {
        super("Account is locked after more than 3 failed login attempts. Request an email OTP to unlock it.");
    }
}
