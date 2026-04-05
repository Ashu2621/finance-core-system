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
@Table(name = "records")
public class FinancialRecord extends BaseEntity {

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private RecordType type;

    private String category;

    private LocalDate date;

    private String note;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserAccount user;
}