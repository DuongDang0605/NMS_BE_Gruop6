package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.SystemAccountRequestDto;
import com.mvc.nms_be.dto.request.SystemAccountUpdateRequestDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import com.mvc.nms_be.dto.request.UserSearchRequestDto;
import com.mvc.nms_be.dto.request.ChangePasswordRequestDto;
import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.exception.ResourceConflictException;
import com.mvc.nms_be.exception.ResourceNotFoundException;
import com.mvc.nms_be.mapper.SystemAccountMapper;
import com.mvc.nms_be.repository.SystemAccountRepository;
import com.mvc.nms_be.repository.SystemAccountSpecifications;
import com.mvc.nms_be.security.SecurityUtils;
import com.mvc.nms_be.service.SystemAccountService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
@Transactional(readOnly = true)
public class SystemAccountServiceImpl implements SystemAccountService {

    private final SystemAccountRepository accountRepository;
    private final SystemAccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public SystemAccountResponseDto toggleVerify(Integer id) {
        SecurityUtils.requireAdmin();
        SystemAccount account = findAccountForUpdate(id);
        if (Integer.valueOf(SystemAccount.ADMIN_ROLE).equals(account.getAccountRole())
                && Boolean.TRUE.equals(account.getIsVerified())) {
            throw new ResourceConflictException("Admin accounts cannot be unverified", "isVerified");
        }
        account.setIsVerified(!Boolean.TRUE.equals(account.getIsVerified()));
        return accountMapper.toResponseDto(accountRepository.saveAndFlush(account));
    }

    @Override
    public SystemAccountResponseDto me() {
        return accountMapper.toResponseDto(findAccount(SecurityUtils.getCurrentAccountId()));
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequestDto request) {
        SystemAccount account = findAccountForUpdate(SecurityUtils.getCurrentAccountId());
        if (!passwordEncoder.matches(request.currentPassword(), account.getAccountPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), account.getAccountPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must differ from the old password");
        }
        account.setAccountPassword(passwordEncoder.encode(request.newPassword()));
        // Invalidate any outstanding recovery challenge after an authenticated password change.
        account.clearUnlockChallenge();
    }

    @Override
    public Page<SystemAccountResponseDto> search(UserSearchRequestDto filter, Pageable pageable) {
        SecurityUtils.requireAdmin();
        if (pageable.isUnpaged() || pageable.getPageSize() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
        }
        var allowedSorts = Set.of("accountId", "accountName", "accountEmail", "accountRole", "isVerified");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sort field must be accountId, accountName, accountEmail, accountRole or isVerified");
            }
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("accountId");
        if (sort.getOrderFor("accountId") == null) {
            sort = sort.and(Sort.by("accountId"));
        }
        var result = accountRepository.findAll(SystemAccountSpecifications.matching(filter),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));
        return result.map(accountMapper::toResponseDto);
    }

    @Override
    public List<SystemAccountResponseDto> findAll() {
        SecurityUtils.requireAdmin();
        return accountRepository.findAll(Sort.by("accountId")).stream()
                .map(accountMapper::toResponseDto)
                .toList();
    }

    @Override
    public SystemAccountResponseDto getById(Integer id) {
        SecurityUtils.requireAdmin();
        return accountMapper.toResponseDto(findAccount(id));
    }

    @Override
    @Transactional
    public SystemAccountResponseDto create(SystemAccountRequestDto request) {
        SecurityUtils.requireAdmin();
        if (Integer.valueOf(SystemAccount.ADMIN_ROLE).equals(request.getAccountRole())) {
            throw new ResourceConflictException("Admin accounts cannot be created", "accountRole");
        }
        String email = normalizeEmail(request.getAccountEmail());
        if (accountRepository.existsByAccountEmailIgnoreCase(email)) {
            throw duplicateEmail();
        }
        SystemAccount account = accountMapper.toEntity(request, passwordEncoder.encode(request.getAccountPassword()));
        account.setIsVerified(true);
        return accountMapper.toResponseDto(accountRepository.saveAndFlush(account));
    }

    @Override
    @Transactional
    public SystemAccountResponseDto update(Integer id, SystemAccountUpdateRequestDto request) {
        SecurityUtils.requireAdmin();
        SystemAccount account = findAccountForUpdate(id);
        boolean isAdmin = Integer.valueOf(SystemAccount.ADMIN_ROLE).equals(account.getAccountRole());
        boolean requestsAdmin = Integer.valueOf(SystemAccount.ADMIN_ROLE).equals(request.getAccountRole());
        if (isAdmin != requestsAdmin) {
            throw new ResourceConflictException(isAdmin
                    ? "The role of an Admin account cannot be changed"
                    : "Accounts cannot be changed to the Admin role", "accountRole");
        }
        String email = normalizeEmail(request.getAccountEmail());
        if (accountRepository.existsByAccountEmailIgnoreCaseAndAccountIdNot(email, id)) {
            throw duplicateEmail();
        }
        String password = request.getAccountPassword() == null ? account.getAccountPassword()
                : passwordEncoder.encode(request.getAccountPassword());
        if (request.getAccountPassword() != null || !email.equals(account.getAccountEmail())) {
            account.clearUnlockChallenge();
        }
        accountMapper.updateEntity(account, request, password);
        return accountMapper.toResponseDto(accountRepository.saveAndFlush(account));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        SecurityUtils.requireAdmin();
        SystemAccount account = findAccountForUpdate(id);
        if (Integer.valueOf(SystemAccount.ADMIN_ROLE).equals(account.getAccountRole())) {
            throw new ResourceConflictException("Admin accounts cannot be deactivated");
        }
        account.setActive(false);
        account.clearUnlockChallenge();
        accountRepository.saveAndFlush(account);
    }

    @Override
    @Transactional
    public SystemAccountResponseDto activate(Integer id) {
        SecurityUtils.requireAdmin();
        SystemAccount account = findAccountForUpdate(id);
        account.setActive(true);
        return accountMapper.toResponseDto(accountRepository.saveAndFlush(account));
    }

    private SystemAccount findAccount(Integer id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " was not found"));
    }

    private SystemAccount findAccountForUpdate(Integer id) {
        return accountRepository.findForUpdateById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " was not found"));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static ResourceConflictException duplicateEmail() {
        return new ResourceConflictException("Email is already in use", "accountEmail");
    }
}
