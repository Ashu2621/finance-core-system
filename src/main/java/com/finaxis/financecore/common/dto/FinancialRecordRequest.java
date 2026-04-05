package com.finaxis.financecore.common.dto;

import com.finaxis.financecore.record.RecordType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class FinancialRecordRequest {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private RecordType type;

    @NotBlank
    private String category;

    @NotNull
    private LocalDate date;

    private String note;
}