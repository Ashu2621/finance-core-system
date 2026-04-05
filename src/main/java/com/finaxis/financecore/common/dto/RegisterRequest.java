package com.finaxis.financecore.common.dto;

import com.finaxis.financecore.user.UserRole;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String name;

    @Email
    private String email;

    @NotBlank
    private String password;

    private UserRole role; // ADMIN / ANALYST / VIEWER
}