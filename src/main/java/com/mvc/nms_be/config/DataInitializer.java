package com.mvc.nms_be.config;

import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.repository.SystemAccountRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
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
    private final boolean seedEnabled;
    private final boolean migratePlaintext;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(SystemAccountRepository accounts, PasswordEncoder encoder,
            @Value("${app.seed-admin.enabled:true}") boolean seedEnabled,
            @Value("${app.migrate-plaintext-passwords:true}") boolean migratePlaintext,
            @Value("${app.seed-admin.email:admin@fu.edu.vn}") String adminEmail,
            @Value("${app.seed-admin.password:Admin@123}") String adminPassword) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.seedEnabled = seedEnabled;
        this.migratePlaintext = migratePlaintext;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
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
        // Seed only a new, empty database; never reset or elevate an existing account.
        if (seedEnabled && accounts.count() == 0) {
            requireBcryptLength(adminPassword, "Seed admin");
            if (adminPassword.length() < 6 || adminPassword.isBlank()) {
                throw new IllegalArgumentException("Seed admin password must contain at least 6 characters");
            }
            SystemAccount admin = new SystemAccount();
            admin.setAccountName("System Administrator");
            admin.setAccountEmail(adminEmail.strip().toLowerCase(Locale.ROOT));
            admin.setAccountRole(SystemAccount.ADMIN_ROLE);
            admin.setAccountPassword(encoder.encode(adminPassword));
            accounts.save(admin);
        }
        accounts.flush();
    }

    private static void requireBcryptLength(String password, String label) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(label + " needs a password of at most 72 UTF-8 bytes before BCrypt migration");
        }
    }
}
