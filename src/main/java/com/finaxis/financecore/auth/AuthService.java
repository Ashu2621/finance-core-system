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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(this::invalidCredentials);

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidCredentials();
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
}
