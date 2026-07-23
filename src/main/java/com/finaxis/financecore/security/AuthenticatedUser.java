package com.finaxis.financecore.security;

import com.finaxis.financecore.user.UserRole;

public record AuthenticatedUser(Long id, String email, UserRole role) {
}
