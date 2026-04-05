package com.finaxis.financecore.dashboard;

import com.finaxis.financecore.common.dto.DashboardResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/summary")
    public DashboardResponse getSummary(HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        return service.getSummary(userId);
    }
}