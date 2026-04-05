package com.finaxis.financecore.record;

import com.finaxis.financecore.common.dto.FinancialRecordRequest;
import com.finaxis.financecore.common.dto.FinancialRecordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.finaxis.financecore.user.UserRepository;
import com.finaxis.financecore.user.UserAccount;

@Service
@RequiredArgsConstructor
public class FinancialRecordService {

    private final FinancialRecordRepository repository;
    private final UserRepository userRepository;

    // CREATE
    public FinancialRecordResponse create(FinancialRecordRequest request, Long userId) {

        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FinancialRecord record = new FinancialRecord();
        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory());
        record.setDate(request.getDate());
        record.setNote(request.getNote());
        record.setUser(user);

        FinancialRecord saved = repository.save(record);

        return mapToResponse(saved);
    }

    // GET ALL
    public Page<FinancialRecordResponse> getAll(Long userId, Pageable pageable) {
        return repository.findByUserIdAndDeletedAtIsNull(userId, pageable)
                .map(this::mapToResponse);
    }

    // SOFT DELETE
    public void softDelete(Long id, Long userId) {

        FinancialRecord record = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));

        if (record.getDeletedAt() != null) {
            throw new RuntimeException("Record already deleted");
        }


        if (!record.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        record.setDeletedAt(java.time.LocalDateTime.now());
        repository.save(record);
    }
    public FinancialRecordResponse update(Long id, FinancialRecordRequest request, Long userId) {

        FinancialRecord record = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));

        if (record.getDeletedAt() != null) {
            throw new RuntimeException("Cannot update deleted record");
        }


        if (!record.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory());
        record.setDate(request.getDate());
        record.setNote(request.getNote());

        FinancialRecord updated = repository.save(record);

        return mapToResponse(updated);
    }
    public Page<FinancialRecordResponse> filter(
            Long userId,
            RecordType type,
            String category,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        return repository.filterRecords(userId, type, category, startDate, endDate, search, pageable)
                .map(this::mapToResponse);
    }

    // MAPPER
    private FinancialRecordResponse mapToResponse(FinancialRecord record) {
        return FinancialRecordResponse.builder()
                .id(record.getId())
                .amount(record.getAmount())
                .type(record.getType())
                .category(record.getCategory())
                .date(record.getDate())
                .note(record.getNote())
                .build();
    }
}