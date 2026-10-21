package com.mvc.nms_be.config;

import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.repository.SystemAccountRepository;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Pattern BCRYPT = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");
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
                if (!BCRYPT.matcher(account.getAccountPassword()).matches()) {
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
