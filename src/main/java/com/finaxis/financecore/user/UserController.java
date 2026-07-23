package com.finaxis.financecore.user;

import com.finaxis.financecore.common.dto.UpdateUserRoleRequest;
import com.finaxis.financecore.common.dto.UserResponse;
import com.finaxis.financecore.common.dto.PageResponse;
import com.finaxis.financecore.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public PageResponse<UserResponse> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return PageResponse.from(userService.getAll(page, size));
    }

    @PutMapping("/{id}/activate")
    public UserResponse activate(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return userService.setActive(id, true, currentUser.id());
    }

    @PutMapping("/{id}/deactivate")
    public UserResponse deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return userService.setActive(id, false, currentUser.id());
    }

    @PutMapping("/{id}/role")
    public UserResponse updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return userService.updateRole(id, request, currentUser.id());
    }
}
