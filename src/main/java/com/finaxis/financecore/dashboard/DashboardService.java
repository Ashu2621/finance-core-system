package com.finaxis.financecore.dashboard;

import com.finaxis.financecore.common.dto.DashboardResponse;
import com.finaxis.financecore.record.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FinancialRecordRepository repository;

    public DashboardResponse getSummary(Long userId) {

        BigDecimal income = repository.getTotalIncome(userId);
        BigDecimal expense = repository.getTotalExpense(userId);

        BigDecimal balance = income.subtract(expense);

        // category breakdown
        List<Object[]> raw = repository.getCategorySummary(userId);

        Map<String, BigDecimal> categoryMap = new HashMap<>();
        for (Object[] row : raw) {
            String category = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            categoryMap.put(category, amount);
        }

        return DashboardResponse.builder()
                .totalIncome(income)
                .totalExpense(expense)
                .balance(balance)
                .categoryBreakdown(categoryMap)
                .build();
    }
}