package com.finaxis.financecore.common.dto;

import com.finaxis.financecore.user.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotNull UserRole role) {
}
