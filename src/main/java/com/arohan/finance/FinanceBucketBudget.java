package com.arohan.finance;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "finance_bucket_budget")
public class FinanceBucketBudget extends AuditableEntity {
    @Id private UUID id;
    @Column(name = "plan_id", nullable = false) private UUID planId;
    @Column(name = "bucket_id", nullable = false) private UUID bucketId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;

    protected FinanceBucketBudget() {}
    FinanceBucketBudget(UUID planId, UUID bucketId, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.planId = planId;
        this.bucketId = bucketId;
        this.amount = amount;
    }
    UUID getId() { return id; }
    UUID getPlanId() { return planId; }
    UUID getBucketId() { return bucketId; }
    BigDecimal getAmount() { return amount; }
    void changeAmount(BigDecimal amount) { this.amount = amount; }
}
