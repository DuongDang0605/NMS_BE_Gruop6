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
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {
    private final SystemAccountRepository accountRepository;

    @Override
    public CustomUserDetails loadUserByUsername(String email) {
        return accountRepository.findByAccountEmailIgnoreCase(email.strip().toLowerCase(Locale.ROOT))
                .filter(account -> account.getAccountRole() != null
                        && (account.getAccountRole() == SystemAccount.ADMIN_ROLE
                        || account.getAccountRole() == SystemAccount.STAFF_ROLE))
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }

    public CustomUserDetails loadUserById(Integer id) {
        return accountRepository.findById(id)
                .filter(account -> account.getAccountRole() != null
                        && (account.getAccountRole() == SystemAccount.ADMIN_ROLE
                        || account.getAccountRole() == SystemAccount.STAFF_ROLE))
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Account no longer exists"));
    }
}
