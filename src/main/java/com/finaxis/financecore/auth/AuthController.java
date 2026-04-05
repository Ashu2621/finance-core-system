package com.finaxis.financecore.auth;

import com.finaxis.financecore.common.dto.AuthResponse;
import com.finaxis.financecore.common.dto.LoginRequest;
import com.finaxis.financecore.common.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        String token = authService.login(request);
        return AuthResponse.builder().token(token).build();
    }
    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return "User registered successfully";
    }
}