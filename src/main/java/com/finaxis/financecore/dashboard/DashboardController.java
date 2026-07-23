package com.finaxis.financecore.dashboard;

import com.finaxis.financecore.common.dto.DashboardResponse;
import com.finaxis.financecore.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/summary")
    public DashboardResponse getSummary(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.getSummary(user.id());
    }
}
