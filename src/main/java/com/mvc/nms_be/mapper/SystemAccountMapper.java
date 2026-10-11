package com.mvc.nms_be.mapper;

import com.mvc.nms_be.dto.request.SystemAccountRequestDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import com.mvc.nms_be.entity.SystemAccount;
import org.springframework.stereotype.Component;

@Component
public class SystemAccountMapper {

    // The service hashes request.accountPassword before calling this method.
    public SystemAccount toEntity(SystemAccountRequestDto request, String passwordHash) {
        SystemAccount account = new SystemAccount();
        account.setAccountName(request.getAccountName());
        account.setAccountEmail(request.getAccountEmail());
        account.setAccountRole(request.getAccountRole());
        account.setAccountPassword(passwordHash);
        return account;
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
