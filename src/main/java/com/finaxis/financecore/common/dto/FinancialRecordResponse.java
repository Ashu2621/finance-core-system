package com.finaxis.financecore.common.dto;

import com.finaxis.financecore.record.RecordType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class FinancialRecordResponse {

    private Long id;
    private BigDecimal amount;
    private RecordType type;
    private String category;
    private LocalDate date;
    private String note;
}