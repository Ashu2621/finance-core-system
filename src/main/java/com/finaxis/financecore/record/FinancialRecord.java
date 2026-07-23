package com.finaxis.financecore.record;

import com.finaxis.financecore.common.BaseEntity;
import com.finaxis.financecore.user.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(
        name = "records",
        indexes = {
                @Index(name = "idx_records_user_deleted_date", columnList = "user_id, deleted_at, date"),
                @Index(name = "idx_records_user_type", columnList = "user_id, type"),
                @Index(name = "idx_records_category", columnList = "category")
        }
)
public class FinancialRecord extends BaseEntity {

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordType type;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
}
