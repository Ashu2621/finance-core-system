package com.finaxis.financecore.record;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {


    Page<FinancialRecord> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);


    @Query("""
SELECT r FROM FinancialRecord r
WHERE r.deletedAt IS NULL
AND r.user.id = :userId
AND (:type IS NULL OR r.type = :type)
AND (:category IS NULL OR r.category = :category)
AND (:startDate IS NULL OR r.date >= :startDate)
AND (:endDate IS NULL OR r.date <= :endDate)
AND (:search IS NULL OR LOWER(r.note) LIKE LOWER(CONCAT('%', :search, '%')))
""")
    Page<FinancialRecord> filterRecords(
            @Param("userId") Long userId,
            @Param("type") RecordType type,
            @Param("category") String category,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );
    @Query("""
SELECT COALESCE(SUM(r.amount), 0)
FROM FinancialRecord r
WHERE r.deletedAt IS NULL
AND r.user.id = :userId
AND r.type = 'INCOME'
""")
    java.math.BigDecimal getTotalIncome(@Param("userId") Long userId);


    @Query("""
SELECT COALESCE(SUM(r.amount), 0)
FROM FinancialRecord r
WHERE r.deletedAt IS NULL
AND r.user.id = :userId
AND r.type = 'EXPENSE'
""")
    java.math.BigDecimal getTotalExpense(@Param("userId") Long userId);
    @Query("""
SELECT r.category, SUM(r.amount)
FROM FinancialRecord r
WHERE r.deletedAt IS NULL
AND r.user.id = :userId
GROUP BY r.category
""")
    java.util.List<Object[]> getCategorySummary(@Param("userId") Long userId);
}