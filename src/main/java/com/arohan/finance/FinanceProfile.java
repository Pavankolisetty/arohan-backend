package com.arohan.finance;

import com.arohan.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "finance_profile")
public class FinanceProfile extends AuditableEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "currency_code", nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currencyCode;

    protected FinanceProfile() {}

    FinanceProfile(UUID userId, String currencyCode) {
        this.userId = userId;
        this.currencyCode = currencyCode;
    }

    UUID getUserId() { return userId; }
    String getCurrencyCode() { return currencyCode; }
    void changeCurrency(String currencyCode) { this.currencyCode = currencyCode; }
}
