package com.finaxis.financecore.user;

import com.finaxis.financecore.audit.AuditService;
import com.finaxis.financecore.common.dto.UpdateUserRoleRequest;
import com.finaxis.financecore.common.dto.UserResponse;
import com.finaxis.financecore.common.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(int page, int size) {
        return userRepository.findAll(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(UserResponse::from);
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active, Long actingUserId) {
        if (!active && id.equals(actingUserId)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SELF_DEACTIVATION",
                    "You cannot deactivate your own account"
            );
        }
        UserAccount user = findUser(id);
        user.setActive(active);
        UserAccount saved = userRepository.save(user);
        auditService.record(
                actingUserId,
                active ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                "USER_ACCOUNT",
                saved.getId()
        );
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse updateRole(Long id, UpdateUserRoleRequest request, Long actingUserId) {
        if (id.equals(actingUserId) && request.role() != UserRole.ADMIN) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SELF_ROLE_CHANGE",
                    "You cannot remove your own administrator role"
            );
        }
        UserAccount user = findUser(id);
        user.setRole(request.role());
        UserAccount saved = userRepository.save(user);
        auditService.record(actingUserId, "USER_ROLE_CHANGED", "USER_ACCOUNT", saved.getId());
        return UserResponse.from(saved);
    }

    private UserAccount findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found"
                ));
    }
}
