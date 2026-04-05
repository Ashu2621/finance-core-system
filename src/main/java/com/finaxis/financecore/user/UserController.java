package com.finaxis.financecore.user;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // GET ALL USERS
    @GetMapping
    public List<UserAccount> getAll() {
        return userRepository.findAll();
    }

    // ACTIVATE USER
    @PutMapping("/{id}/activate")
    public String activate(@PathVariable Long id) {
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(true);
        userRepository.save(user);

        return "User activated";
    }

    // DEACTIVATE USER
    @PutMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id) {
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(false);
        userRepository.save(user);

        return "User deactivated";
    }
}