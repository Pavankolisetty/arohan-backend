package com.arohan.finance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FinanceDtos {
    private FinanceDtos() {}

    public record BucketResponse(UUID id, String systemKey, String name, String colorHex,
                                 String iconKey, int positionIndex, boolean active) {}
    public record CategoryResponse(UUID id, UUID bucketId, String name, int positionIndex,
                                   boolean active) {}
    public record SetupResponse(String currencyCode, List<BucketResponse> buckets,
                                List<CategoryResponse> categories) {}

    public record UpdateProfileRequest(
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Use a three-letter currency code.")
        String currencyCode) {}

    public record UpdateBucketRequest(
        @NotBlank @Size(max = 60) String name,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String colorHex,
        @NotBlank @Size(max = 40) String iconKey,
        int positionIndex) {}

    public record CategoryRequest(@NotNull UUID bucketId,
                                  @NotBlank @Size(max = 80) String name,
                                  int positionIndex, boolean active) {}

    public record TransactionRequest(
        @NotNull FinanceTransactionType type,
        UUID bucketId,
        UUID categoryId,
        @NotBlank @Size(max = 120) String title,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull LocalDate occurredOn,
        @NotNull PaymentMode paymentMode,
        TransferDirection transferDirection,
        @Size(max = 120) String incomeSource,
        @Size(max = 500) String note,
        RecurringFrequency recurringFrequency,
        LocalDate recurringUntil) {}

    public record TransactionResponse(
        UUID id, FinanceTransactionType type, UUID bucketId, String bucketName,
        UUID categoryId, String categoryName, String title, BigDecimal amount,
        LocalDate occurredOn, PaymentMode paymentMode, TransferDirection transferDirection,
        String incomeSource, String note, RecurringFrequency recurringFrequency,
        LocalDate recurringUntil) {}

    public record CashAdjustmentRequest(
        @NotNull BigDecimal amount,
        @NotNull LocalDate adjustedOn,
        @NotBlank @Size(max = 240) String reason,
        @NotNull CashAdjustmentKind adjustmentKind) {}
    public record CashAdjustmentResponse(UUID id, BigDecimal amount, LocalDate adjustedOn,
                                         String reason, CashAdjustmentKind adjustmentKind) {}

    public record BucketBudgetInput(@NotNull UUID bucketId,
                                    @NotNull @DecimalMin("0.00") BigDecimal amount) {}
    public record MonthPlanRequest(
        @NotNull @DecimalMin("0.00") BigDecimal expectedIncome,
        @NotNull @DecimalMin("0.00") BigDecimal savingsTarget,
        @Size(max = 500) String intention,
        @Size(max = 700) String wentWell,
        @Size(max = 700) String learned,
        @Size(max = 700) String nextMonthChange,
        @Valid List<BucketBudgetInput> bucketBudgets) {}
    public record MonthPlanResponse(
        LocalDate monthStart, BigDecimal expectedIncome, BigDecimal savingsTarget,
        String intention, String wentWell, String learned, String nextMonthChange,
        Map<UUID, BigDecimal> bucketBudgets) {}

    public record FinanceSummary(BigDecimal income, BigDecimal expenses, BigDecimal refunds,
                                 BigDecimal netExpenses, BigDecimal savings,
                                 BigDecimal available, BigDecimal savingsRatePercent,
                                 BigDecimal cashBalance) {}
    public record BucketFlow(UUID bucketId, String systemKey, String name, String colorHex,
                             String iconKey, BigDecimal budget, BigDecimal spent,
                             BigDecimal refunded, BigDecimal netSpent,
                             BigDecimal remaining, BigDecimal usedPercent) {}
    public record DailyFlow(LocalDate date, BigDecimal income, BigDecimal expenses,
                            BigDecimal savings) {}
    public record DashboardResponse(String currencyCode, LocalDate monthStart,
                                    FinanceSummary summary, MonthPlanResponse plan,
                                    List<BucketFlow> bucketFlows, List<DailyFlow> rhythm,
                                    List<TransactionResponse> transactions,
                                    List<CashAdjustmentResponse> cashAdjustments) {}

    public record FinanceInsightBucket(
        UUID bucketId, String systemKey, String name, String colorHex,
        BigDecimal netSpent, BigDecimal percentOfIncome, BigDecimal percentOfSpending) {}
    public record FinanceInsightPoint(
        LocalDate periodStart, String label, BigDecimal income, BigDecimal savings,
        BigDecimal needs, BigDecimal wants, BigDecimal experiences,
        BigDecimal unexpected, BigDecimal available) {}
    public record FinanceInsightsResponse(
        String currencyCode, String period, LocalDate from, LocalDate to,
        FinanceSummary summary, List<FinanceInsightBucket> buckets,
        List<FinanceInsightPoint> timeline) {}
}
