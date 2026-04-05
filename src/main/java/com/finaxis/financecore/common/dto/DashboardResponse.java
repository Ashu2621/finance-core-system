package com.finaxis.financecore.common.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class DashboardResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private Map<String, BigDecimal> categoryBreakdown;
}