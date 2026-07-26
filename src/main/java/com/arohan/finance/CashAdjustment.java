package com.arohan.finance;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "finance_cash_adjustment")
public class CashAdjustment extends AuditableEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "adjusted_on", nullable = false) private LocalDate adjustedOn;
    @Column(nullable = false, length = 240) private String reason;
    @Enumerated(EnumType.STRING) @Column(name = "adjustment_kind", nullable = false, length = 16)
    private CashAdjustmentKind adjustmentKind;

    protected CashAdjustment() {}
    CashAdjustment(UUID userId, BigDecimal amount, LocalDate adjustedOn, String reason,
                   CashAdjustmentKind adjustmentKind) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.amount = amount;
        this.adjustedOn = adjustedOn;
        this.reason = reason;
        this.adjustmentKind = adjustmentKind;
    }
    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    BigDecimal getAmount() { return amount; }
    LocalDate getAdjustedOn() { return adjustedOn; }
    String getReason() { return reason; }
    CashAdjustmentKind getAdjustmentKind() { return adjustmentKind; }
}
