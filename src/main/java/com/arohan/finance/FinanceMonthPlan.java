package com.arohan.finance;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "finance_month_plan")
public class FinanceMonthPlan extends AuditableEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "month_start", nullable = false) private LocalDate monthStart;
    @Column(name = "expected_income", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedIncome;
    @Column(name = "savings_target", nullable = false, precision = 19, scale = 2)
    private BigDecimal savingsTarget;
    @Column(length = 500) private String intention;
    @Column(name = "went_well", length = 700) private String wentWell;
    @Column(length = 700) private String learned;
    @Column(name = "next_month_change", length = 700) private String nextMonthChange;

    protected FinanceMonthPlan() {}
    FinanceMonthPlan(UUID userId, LocalDate monthStart) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.monthStart = monthStart;
        this.expectedIncome = BigDecimal.ZERO;
        this.savingsTarget = BigDecimal.ZERO;
    }
    void update(BigDecimal expectedIncome, BigDecimal savingsTarget, String intention,
                String wentWell, String learned, String nextMonthChange) {
        this.expectedIncome = expectedIncome;
        this.savingsTarget = savingsTarget;
        this.intention = intention;
        this.wentWell = wentWell;
        this.learned = learned;
        this.nextMonthChange = nextMonthChange;
    }
    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    LocalDate getMonthStart() { return monthStart; }
    BigDecimal getExpectedIncome() { return expectedIncome; }
    BigDecimal getSavingsTarget() { return savingsTarget; }
    String getIntention() { return intention; }
    String getWentWell() { return wentWell; }
    String getLearned() { return learned; }
    String getNextMonthChange() { return nextMonthChange; }
}
