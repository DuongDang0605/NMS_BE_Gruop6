package com.mvc.nms_be.service.impl;

import com.mvc.nms_be.dto.request.SystemAccountRequestDto;
import com.mvc.nms_be.dto.request.SystemAccountUpdateRequestDto;
import com.mvc.nms_be.dto.response.SystemAccountResponseDto;
import com.mvc.nms_be.dto.request.UserSearchRequestDto;
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
    public Page<SystemAccountResponseDto> search(UserSearchRequestDto filter, Pageable pageable) {
        SecurityUtils.requireAdmin();
        if (pageable.isUnpaged() || pageable.getPageSize() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
        }
        var allowedSorts = Set.of("accountId", "accountName", "accountEmail", "accountRole");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sort field must be accountId, accountName, accountEmail or accountRole");
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
        String email = normalizeEmail(request.getAccountEmail());
        if (accountRepository.existsByAccountEmailIgnoreCase(email)) {
            throw duplicateEmail();
        }
        SystemAccount account = accountMapper.toEntity(request, passwordEncoder.encode(request.getAccountPassword()));
        return accountMapper.toResponseDto(accountRepository.saveAndFlush(account));
    }

    @Override
    @Transactional
    public SystemAccountResponseDto update(Integer id, SystemAccountUpdateRequestDto request) {
        SecurityUtils.requireAdmin();
        SystemAccount account = findAccount(id);
        String email = normalizeEmail(request.getAccountEmail());
        if (accountRepository.existsByAccountEmailIgnoreCaseAndAccountIdNot(email, id)) {
            throw duplicateEmail();
        }
        String password = request.getAccountPassword() == null ? account.getAccountPassword()
                : passwordEncoder.encode(request.getAccountPassword());
        accountMapper.updateEntity(account, request, password);
        return accountMapper.toResponseDto(accountRepository.saveAndFlush(account));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        SecurityUtils.requireAdmin();
        SystemAccount account = findAccount(id);
        accountRepository.delete(account);
        // Flush inside the request so FK conflicts can be returned as HTTP 409.
        accountRepository.flush();
    }

    private SystemAccount findAccount(Integer id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " was not found"));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static ResourceConflictException duplicateEmail() {
        return new ResourceConflictException("Email is already in use", "accountEmail");
    }
}
