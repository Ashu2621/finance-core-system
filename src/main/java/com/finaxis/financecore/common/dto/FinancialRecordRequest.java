package com.finaxis.financecore.common.dto;

import com.finaxis.financecore.record.RecordType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialRecordRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull RecordType type,
        @NotBlank @Size(max = 80) String category,
        @NotNull @PastOrPresent LocalDate date,
        @Size(max = 500) String note
) {
}
