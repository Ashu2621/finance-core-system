package com.finaxis.financecore.common.dto;

import com.finaxis.financecore.record.RecordType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FinancialRecordResponse(
        Long id,
        BigDecimal amount,
        RecordType type,
        String category,
        LocalDate date,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
