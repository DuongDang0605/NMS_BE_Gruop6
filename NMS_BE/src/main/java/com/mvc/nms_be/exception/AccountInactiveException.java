package com.mvc.nms_be.exception;

public class AccountInactiveException extends UnauthorizedException {
    public AccountInactiveException() {
        super("Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ Admin để biết thêm chi tiết.");
    }
}
