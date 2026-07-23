package com.finaxis.financecore.auth;

import com.finaxis.financecore.common.dto.AuthResponse;
import com.finaxis.financecore.common.dto.LoginRequest;
import com.finaxis.financecore.common.dto.RegisterRequest;
import com.finaxis.financecore.common.dto.UserResponse;
import com.finaxis.financecore.common.error.ApiException;
import com.finaxis.financecore.user.UserAccount;
import com.finaxis.financecore.user.UserRepository;
import com.finaxis.financecore.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(this::invalidCredentials);

        Instant now = clock.instant();
        if (!user.isActive() || isLocked(user, now)) {
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            registerFailedAttempt(user, now);
            throw invalidCredentials();
        }

        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
        return issueToken(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email is already registered");
        }

        UserAccount user = new UserAccount();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.VIEWER);
        user.setActive(true);

        return issueToken(userRepository.save(user));
    }

    private AuthResponse issueToken(UserAccount user) {
        JwtService.TokenDetails token = jwtService.generateToken(user);
        return new AuthResponse(
                token.token(),
                "Bearer",
                token.expiresAt(),
                UserResponse.from(user)
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidCredentials() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Invalid email or password"
        );
    }

    private boolean isLocked(UserAccount user, Instant now) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
    }

    private void registerFailedAttempt(UserAccount user, Instant now) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(now.plus(LOCK_DURATION));
            user.setFailedLoginAttempts(0);
        }
        userRepository.save(user);
    }
}
