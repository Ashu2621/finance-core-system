package com.finaxis.financecore.record;

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

@Service
@RequiredArgsConstructor
public class FinancialRecordService {

    private final FinancialRecordRepository repository;
    private final UserRepository userRepository;

    @Transactional
    public FinancialRecordResponse create(FinancialRecordRequest request, Long userId) {
        UserAccount user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "USER_INACTIVE",
                        "User account is not active"
                ));

        FinancialRecord record = new FinancialRecord();
        apply(record, request);
        record.setUser(user);
        return mapToResponse(repository.save(record));
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
    }

    @Transactional
    public FinancialRecordResponse update(
            Long id,
            FinancialRecordRequest request,
            Long userId
    ) {
        FinancialRecord record = findOwned(id, userId);
        apply(record, request);
        return mapToResponse(repository.save(record));
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
