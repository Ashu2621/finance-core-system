package com.finaxis.financecore.common.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        long transactionCount,
        Map<String, BigDecimal> categoryBreakdown
) {
}
