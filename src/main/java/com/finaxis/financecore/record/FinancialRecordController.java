package com.finaxis.financecore.record;

import com.finaxis.financecore.common.dto.FinancialRecordRequest;
import com.finaxis.financecore.common.dto.FinancialRecordResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class FinancialRecordController {

    private final FinancialRecordService service;

    // CREATE
    @PostMapping
    public FinancialRecordResponse create(
            @Valid @RequestBody FinancialRecordRequest request,
            HttpServletRequest http
    ) {
        Long userId = (Long) http.getAttribute("userId");

        if (userId == null) {
            throw new RuntimeException("Unauthorized");
        }

        return service.create(request, userId);
    }

    // GET ALL (with pagination)
    @GetMapping
    public Page<FinancialRecordResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest http
    ) {
        Long userId = (Long) http.getAttribute("userId");

        if (userId == null) {
            throw new RuntimeException("Unauthorized");
        }

        return service.getAll(userId, PageRequest.of(page, size));
    }

    // SOFT DELETE
    @DeleteMapping("/{id}")
    public String delete(
            @PathVariable Long id,
            HttpServletRequest http
    ) {
        Long userId = (Long) http.getAttribute("userId");

        if (userId == null) {
            throw new RuntimeException("Unauthorized");
        }

        service.softDelete(id, userId);
        return "Record deleted successfully";
    }

    // UPDATE
    @PutMapping("/{id}")
    public FinancialRecordResponse update(
            @PathVariable Long id,
            @Valid @RequestBody FinancialRecordRequest request,
            HttpServletRequest http
    ) {
        Long userId = (Long) http.getAttribute("userId");

        if (userId == null) {
            throw new RuntimeException("Unauthorized");
        }

        return service.update(id, request, userId);
    }

    // FILTER + SEARCH
    @GetMapping("/filter")
    public Page<FinancialRecordResponse> filter(
            @RequestParam(required = false) RecordType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest http
    ) {
        Long userId = (Long) http.getAttribute("userId");

        if (userId == null) {
            throw new RuntimeException("Unauthorized");
        }

        return service.filter(
                userId,
                type,
                category,
                startDate,
                endDate,
                search,
                PageRequest.of(page, size)
        );
    }
}