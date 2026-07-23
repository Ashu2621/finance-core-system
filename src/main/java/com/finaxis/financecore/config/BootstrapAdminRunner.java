package com.finaxis.financecore.config;

import com.finaxis.financecore.user.UserAccount;
import com.finaxis.financecore.user.UserRepository;
import com.finaxis.financecore.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.email())
                || !StringUtils.hasText(properties.password())) {
            return;
        }
        if (properties.password().length() < 12) {
            log.warn("ADMIN_PASSWORD is configured but shorter than 12 characters; bootstrap skipped");
            return;
        }

        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        UserAccount admin = new UserAccount();
        admin.setName(StringUtils.hasText(properties.name()) ? properties.name().trim() : "Administrator");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(properties.password()));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
        log.info("Bootstrap administrator account created for {}", email);
    }
}
