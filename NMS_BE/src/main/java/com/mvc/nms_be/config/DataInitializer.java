package com.mvc.nms_be.config;

import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.repository.SystemAccountRepository;
import com.mvc.nms_be.security.PasswordSupport;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {
    private final SystemAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final boolean migratePlaintext;

    public DataInitializer(SystemAccountRepository accounts, PasswordEncoder encoder,
            @Value("${app.migrate-plaintext-passwords:true}") boolean migratePlaintext) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.migratePlaintext = migratePlaintext;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (migratePlaintext) {
            for (SystemAccount account : accounts.findAll()) {
                if (!PasswordSupport.isBcryptHash(account.getAccountPassword())) {
                    requireBcryptLength(account.getAccountPassword(), "Account " + account.getAccountId());
                    account.setAccountPassword(encoder.encode(account.getAccountPassword()));
                }
            }
        }
        accounts.flush();
    }

    private static void requireBcryptLength(String password, String label) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(label + " needs a password of at most 72 UTF-8 bytes before BCrypt migration");
        }
    }
}
