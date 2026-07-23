package com.finaxis.financecore.record;

import com.finaxis.financecore.common.dto.FinancialRecordRequest;
import com.finaxis.financecore.common.dto.FinancialRecordResponse;
import com.finaxis.financecore.common.dto.PageResponse;
import com.finaxis.financecore.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class FinancialRecordController {

    private final FinancialRecordService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FinancialRecordResponse create(
            @Valid @RequestBody FinancialRecordRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return service.create(request, user.id());
    }

    @GetMapping
    public PageResponse<FinancialRecordResponse> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return PageResponse.from(service.getAll(user.id(), pageRequest(page, size)));
    }

    @GetMapping("/{id}")
    public FinancialRecordResponse getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return service.getById(id, user.id());
    }

    @PutMapping("/{id}")
    public FinancialRecordResponse update(
            @PathVariable Long id,
            @Valid @RequestBody FinancialRecordRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return service.update(id, request, user.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        service.softDelete(id, user.id());
    }

    @GetMapping("/filter")
    public PageResponse<FinancialRecordResponse> filter(
            @RequestParam(required = false) RecordType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return PageResponse.from(service.filter(
                user.id(),
                type,
                category,
                startDate,
                endDate,
                search,
                pageRequest(page, size)
        ));
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("date"), Sort.Order.desc("createdAt"))
        );
    }
}
