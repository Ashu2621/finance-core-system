package com.finaxis.financecore.record;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    Page<FinancialRecord> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    Optional<FinancialRecord> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    long countByUserIdAndDeletedAtIsNull(Long userId);

    @Query("""
            SELECT r FROM FinancialRecord r
            WHERE r.deletedAt IS NULL
              AND r.user.id = :userId
              AND (:type IS NULL OR r.type = :type)
              AND (:category IS NULL OR LOWER(r.category) = LOWER(:category))
              AND (:startDate IS NULL OR r.date >= :startDate)
              AND (:endDate IS NULL OR r.date <= :endDate)
              AND (
                    :search IS NULL
                    OR LOWER(r.note) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(r.category) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<FinancialRecord> filterRecords(
            @Param("userId") Long userId,
            @Param("type") RecordType type,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM FinancialRecord r
            WHERE r.deletedAt IS NULL
              AND r.user.id = :userId
              AND r.type = com.finaxis.financecore.record.RecordType.INCOME
            """)
    BigDecimal getTotalIncome(@Param("userId") Long userId);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM FinancialRecord r
            WHERE r.deletedAt IS NULL
              AND r.user.id = :userId
              AND r.type = com.finaxis.financecore.record.RecordType.EXPENSE
            """)
    BigDecimal getTotalExpense(@Param("userId") Long userId);

    @Query("""
            SELECT r.category, SUM(r.amount)
            FROM FinancialRecord r
            WHERE r.deletedAt IS NULL
              AND r.user.id = :userId
              AND r.type = com.finaxis.financecore.record.RecordType.EXPENSE
            GROUP BY r.category
            ORDER BY SUM(r.amount) DESC
            """)
    List<Object[]> getExpenseCategorySummary(@Param("userId") Long userId);
}
