package com.mvc.nms_be.mapper;

import com.mvc.nms_be.dto.request.SystemAccountRequestDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import com.mvc.nms_be.entity.SystemAccount;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SystemAccountMapper {

    // Password hashing will be added in the service with Spring Security.
    public SystemAccount toEntity(SystemAccountRequestDto request, String password) {
        SystemAccount account = new SystemAccount();
        updateEntity(account, request, password);
        return account;
    }

    public void updateEntity(SystemAccount account, SystemAccountRequestDto request, String password) {
        account.setAccountName(request.getAccountName().strip());
        account.setAccountEmail(request.getAccountEmail().strip().toLowerCase(Locale.ROOT));
        account.setAccountRole(request.getAccountRole());
        account.setAccountPassword(password);
    }

    public SystemAccountResponseDto toResponseDto(SystemAccount account) {
        return new SystemAccountResponseDto(
                account.getAccountId(),
                account.getAccountName(),
                account.getAccountEmail(),
                account.getAccountRole()
        );
    }
}
