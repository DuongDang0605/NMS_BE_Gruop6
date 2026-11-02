package com.mvc.nms_be.security;

import com.mvc.nms_be.repository.SystemAccountRepository;
import com.mvc.nms_be.entity.SystemAccount;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
// A missing username is an expected authentication result, including inside a login transaction.
@Transactional(readOnly = true, noRollbackFor = UsernameNotFoundException.class)
public class CustomUserDetailsService implements UserDetailsService {
    private final SystemAccountRepository accountRepository;

    @Override
    public CustomUserDetails loadUserByUsername(String email) {
        return accountRepository.findByAccountEmailIgnoreCase(email.strip().toLowerCase(Locale.ROOT))
                .filter(account -> SystemAccount.isSupportedRole(account.getAccountRole()))
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }

    public CustomUserDetails loadUserById(Integer id) {
        return accountRepository.findById(id)
                .filter(account -> SystemAccount.isSupportedRole(account.getAccountRole()))
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Account no longer exists"));
    }
}
