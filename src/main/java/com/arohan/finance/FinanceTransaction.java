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
@Table(name = "finance_transaction")
public class FinanceTransaction extends AuditableEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private FinanceTransactionType type;
    @Column(name = "bucket_id") private UUID bucketId;
    @Column(name = "category_id") private UUID categoryId;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "occurred_on", nullable = false) private LocalDate occurredOn;
    @Enumerated(EnumType.STRING) @Column(name = "payment_mode", nullable = false, length = 24)
    private PaymentMode paymentMode;
    @Enumerated(EnumType.STRING) @Column(name = "transfer_direction", length = 16)
    private TransferDirection transferDirection;
    @Column(name = "income_source", length = 120) private String incomeSource;
    @Column(length = 500) private String note;
    @Enumerated(EnumType.STRING) @Column(name = "recurring_frequency", length = 16)
    private RecurringFrequency recurringFrequency;
    @Column(name = "recurring_until") private LocalDate recurringUntil;

    protected FinanceTransaction() {}

    FinanceTransaction(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
    }

    void update(FinanceTransactionType type, UUID bucketId, UUID categoryId, String title,
                BigDecimal amount, LocalDate occurredOn, PaymentMode paymentMode,
                TransferDirection transferDirection, String incomeSource, String note,
                RecurringFrequency recurringFrequency, LocalDate recurringUntil) {
        this.type = type;
        this.bucketId = bucketId;
        this.categoryId = categoryId;
        this.title = title;
        this.amount = amount;
        this.occurredOn = occurredOn;
        this.paymentMode = paymentMode;
        this.transferDirection = transferDirection;
        this.incomeSource = incomeSource;
        this.note = note;
        this.recurringFrequency = recurringFrequency;
        this.recurringUntil = recurringUntil;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    FinanceTransactionType getType() { return type; }
    UUID getBucketId() { return bucketId; }
    UUID getCategoryId() { return categoryId; }
    String getTitle() { return title; }
    BigDecimal getAmount() { return amount; }
    LocalDate getOccurredOn() { return occurredOn; }
    PaymentMode getPaymentMode() { return paymentMode; }
    TransferDirection getTransferDirection() { return transferDirection; }
    String getIncomeSource() { return incomeSource; }
    String getNote() { return note; }
    RecurringFrequency getRecurringFrequency() { return recurringFrequency; }
    LocalDate getRecurringUntil() { return recurringUntil; }
}
