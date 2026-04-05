package com.finaxis.financecore.auth;

import com.finaxis.financecore.common.dto.LoginRequest;
import com.finaxis.financecore.common.dto.RegisterRequest;
import com.finaxis.financecore.user.UserAccount;
import com.finaxis.financecore.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    // LOGIN
    public String login(LoginRequest request) {

        UserAccount user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.isActive()) {
            throw new RuntimeException("User is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtService.generateToken(user.getId(), user.getRole().name());
    }


    public void register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        UserAccount user = new UserAccount();
        user.setName(request.getName());
        user.setEmail(request.getEmail());


        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole(request.getRole());

        userRepository.save(user);
    }
}