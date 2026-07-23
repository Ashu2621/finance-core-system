package com.finaxis.financecore.dashboard;

import com.finaxis.financecore.common.dto.DashboardResponse;
import com.finaxis.financecore.record.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FinancialRecordRepository repository;

    @Transactional(readOnly = true)
    public DashboardResponse getSummary(Long userId) {
        BigDecimal income = repository.getTotalIncome(userId);
        BigDecimal expense = repository.getTotalExpense(userId);

        Map<String, BigDecimal> categoryMap = new LinkedHashMap<>();
        repository.getExpenseCategorySummary(userId).forEach(row ->
                categoryMap.put((String) row[0], (BigDecimal) row[1])
        );

        return new DashboardResponse(
                income,
                expense,
                income.subtract(expense),
                repository.countByUserIdAndDeletedAtIsNull(userId),
                categoryMap
        );
    }
}
