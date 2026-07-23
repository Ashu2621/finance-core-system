package com.finaxis.financecore.record;

import com.finaxis.financecore.audit.AuditService;
import com.finaxis.financecore.common.dto.FinancialRecordRequest;
import com.finaxis.financecore.common.dto.FinancialRecordResponse;
import com.finaxis.financecore.common.error.ApiException;
import com.finaxis.financecore.user.UserAccount;
import com.finaxis.financecore.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Clock;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class FinancialRecordService {

    private final FinancialRecordRepository repository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final Clock clock;

    @Transactional
    public FinancialRecordResponse create(
            FinancialRecordRequest request,
            Long userId,
            String idempotencyKey
    ) {
        validateIdempotencyKey(idempotencyKey);
        String requestHash = hash(request);
        UserAccount user = userRepository.findActiveByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "USER_INACTIVE",
                        "User account is not active"
                ));
        IdempotencyRecord existing = idempotencyRepository
                .findByUserIdAndKey(userId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "IDEMPOTENCY_KEY_REUSED",
                        "Idempotency-Key was already used for a different request"
                );
            }
            return mapToResponse(existing.getRecord());
        }

        FinancialRecord record = new FinancialRecord();
        apply(record, request);
        record.setUser(user);
        FinancialRecord saved = repository.save(record);
        IdempotencyRecord idempotency = new IdempotencyRecord();
        idempotency.setUser(user);
        idempotency.setKey(idempotencyKey);
        idempotency.setRequestHash(requestHash);
        idempotency.setRecord(saved);
        idempotency.setCreatedAt(Instant.now(clock));
        idempotencyRepository.save(idempotency);
        auditService.record(userId, "RECORD_CREATED", "FINANCIAL_RECORD", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<FinancialRecordResponse> getAll(Long userId, Pageable pageable) {
        return repository.findByUserIdAndDeletedAtIsNull(userId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public FinancialRecordResponse getById(Long id, Long userId) {
        return mapToResponse(findOwned(id, userId));
    }

    @Transactional
    public void softDelete(Long id, Long userId) {
        FinancialRecord record = findOwned(id, userId);
        record.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        repository.save(record);
        auditService.record(userId, "RECORD_DELETED", "FINANCIAL_RECORD", record.getId());
    }

    @Transactional
    public FinancialRecordResponse update(
            Long id,
            FinancialRecordRequest request,
            Long userId
    ) {
        FinancialRecord record = findOwned(id, userId);
        apply(record, request);
        FinancialRecord saved = repository.save(record);
        auditService.record(userId, "RECORD_UPDATED", "FINANCIAL_RECORD", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<FinancialRecordResponse> filter(
            Long userId,
            RecordType type,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE",
                    "startDate must be before or equal to endDate"
            );
        }

        return repository.filterRecords(
                        userId,
                        type,
                        normalizeOptional(category),
                        startDate,
                        endDate,
                        normalizeOptional(search),
                        pageable
                )
                .map(this::mapToResponse);
    }

    private FinancialRecord findOwned(Long id, Long userId) {
        return repository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "RECORD_NOT_FOUND",
                        "Financial record not found"
                ));
    }

    private void apply(FinancialRecord record, FinancialRecordRequest request) {
        record.setAmount(request.amount());
        record.setType(request.type());
        record.setCategory(request.category().trim());
        record.setDate(request.date());
        record.setNote(normalizeOptional(request.note()));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || !key.matches("^[A-Za-z0-9._:-]{8,128}$")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key must be 8-128 URL-safe characters"
            );
        }
    }

    private String hash(FinancialRecordRequest request) {
        String canonical = "%s|%s|%s|%s|%s".formatted(
                request.amount().stripTrailingZeros().toPlainString(),
                request.type(),
                request.category().trim(),
                request.date(),
                normalizeOptional(request.note())
        );
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private FinancialRecordResponse mapToResponse(FinancialRecord record) {
        return new FinancialRecordResponse(
                record.getId(),
                record.getAmount(),
                record.getType(),
                record.getCategory(),
                record.getDate(),
                record.getNote(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
