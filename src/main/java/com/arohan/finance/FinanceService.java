package com.arohan.finance;

import static com.arohan.finance.FinanceDtos.*;

import com.arohan.shared.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final FinanceProfileRepository profiles;
    private final FinanceBucketRepository buckets;
    private final FinanceCategoryRepository categories;
    private final FinanceTransactionRepository transactions;
    private final CashAdjustmentRepository adjustments;
    private final FinanceMonthPlanRepository plans;
    private final FinanceBucketBudgetRepository budgets;

    public FinanceService(FinanceProfileRepository profiles, FinanceBucketRepository buckets,
                          FinanceCategoryRepository categories,
                          FinanceTransactionRepository transactions,
                          CashAdjustmentRepository adjustments,
                          FinanceMonthPlanRepository plans,
                          FinanceBucketBudgetRepository budgets) {
        this.profiles = profiles;
        this.buckets = buckets;
        this.categories = categories;
        this.transactions = transactions;
        this.adjustments = adjustments;
        this.plans = plans;
        this.budgets = budgets;
    }

    @Transactional
    public SetupResponse setup(UUID userId) {
        ensureSetup(userId);
        return new SetupResponse(profiles.findById(userId).orElseThrow().getCurrencyCode(),
            buckets.findAllByUserIdOrderByPositionIndexAsc(userId).stream()
                .map(this::bucketResponse).toList(),
            categories.findAllByUserIdOrderByPositionIndexAsc(userId).stream()
                .map(this::categoryResponse).toList());
    }

    @Transactional
    public SetupResponse changeCurrency(UUID userId, UpdateProfileRequest request) {
        ensureSetup(userId);
        FinanceProfile profile = profiles.findById(userId).orElseThrow();
        String code = request.currencyCode().toUpperCase(Locale.ROOT);
        if (!code.equals(profile.getCurrencyCode()) && transactions.existsByUserId(userId)) {
            throw new ApiException(HttpStatus.CONFLICT,
                "Currency cannot be changed after money has been recorded.");
        }
        profile.changeCurrency(code);
        return setup(userId);
    }

    @Transactional
    public BucketResponse updateBucket(UUID userId, UUID id, UpdateBucketRequest request) {
        FinanceBucket bucket = requireBucket(userId, id);
        bucket.update(request.name().trim(), request.colorHex().toUpperCase(Locale.ROOT),
            request.iconKey().trim(), request.positionIndex());
        return bucketResponse(bucket);
    }

    @Transactional
    public CategoryResponse createCategory(UUID userId, CategoryRequest request) {
        requireBucket(userId, request.bucketId());
        if (categories.existsByUserIdAndBucketIdAndNameIgnoreCase(
                userId, request.bucketId(), request.name().trim())) {
            throw new ApiException(HttpStatus.CONFLICT,
                "That category already exists in this bucket.");
        }
        return categoryResponse(categories.save(new FinanceCategory(userId, request.bucketId(),
            request.name().trim(), request.positionIndex())));
    }

    @Transactional
    public CategoryResponse updateCategory(UUID userId, UUID id, CategoryRequest request) {
        FinanceCategory category = categories.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Financial category was not found."));
        requireBucket(userId, request.bucketId());
        category.update(request.name().trim(), request.bucketId(), request.positionIndex(),
            request.active());
        return categoryResponse(category);
    }

    @Transactional
    public TransactionResponse createTransaction(UUID userId, TransactionRequest request) {
        ensureSetup(userId);
        FinanceTransaction transaction = new FinanceTransaction(userId);
        apply(userId, transaction, request);
        return transactionResponse(transactions.save(transaction), lookupBuckets(userId),
            lookupCategories(userId));
    }

    @Transactional
    public TransactionResponse updateTransaction(UUID userId, UUID id,
                                                  TransactionRequest request) {
        FinanceTransaction transaction = transactions.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Money entry was not found."));
        apply(userId, transaction, request);
        return transactionResponse(transaction, lookupBuckets(userId), lookupCategories(userId));
    }

    @Transactional
    public void deleteTransaction(UUID userId, UUID id) {
        FinanceTransaction transaction = transactions.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Money entry was not found."));
        transactions.delete(transaction);
    }

    @Transactional
    public CashAdjustmentResponse adjustCash(UUID userId, CashAdjustmentRequest request) {
        if (request.amount().compareTo(BigDecimal.ZERO) == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Cash adjustment must add or remove an amount.");
        }
        CashAdjustment saved = adjustments.save(new CashAdjustment(userId,
            money(request.amount()), request.adjustedOn(), request.reason().trim(),
            request.adjustmentKind()));
        return adjustmentResponse(saved);
    }

    @Transactional
    public MonthPlanResponse savePlan(UUID userId, YearMonth month, MonthPlanRequest request) {
        ensureSetup(userId);
        LocalDate monthStart = month.atDay(1);
        FinanceMonthPlan plan = plans.findByUserIdAndMonthStart(userId, monthStart)
            .orElseGet(() -> plans.save(new FinanceMonthPlan(userId, monthStart)));
        plan.update(money(request.expectedIncome()), money(request.savingsTarget()),
            trim(request.intention()), trim(request.wentWell()), trim(request.learned()),
            trim(request.nextMonthChange()));
        Map<UUID, BigDecimal> unique = new LinkedHashMap<>();
        if (request.bucketBudgets() != null) {
            for (BucketBudgetInput input : request.bucketBudgets()) {
                requireBucket(userId, input.bucketId());
                if (unique.put(input.bucketId(), money(input.amount())) != null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Each bucket can have only one monthly budget.");
                }
            }
        }
        for (FinanceBucket bucket : buckets.findAllByUserIdOrderByPositionIndexAsc(userId)) {
            BigDecimal amount = unique.getOrDefault(bucket.getId(), ZERO);
            FinanceBucketBudget budget = budgets.findByPlanIdAndBucketId(plan.getId(), bucket.getId())
                .orElseGet(() -> new FinanceBucketBudget(plan.getId(), bucket.getId(), ZERO));
            budget.changeAmount(amount);
            budgets.save(budget);
        }
        return planResponse(plan);
    }

    @Transactional
    public DashboardResponse dashboard(UUID userId, YearMonth month) {
        ensureSetup(userId);
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        List<FinanceTransaction> entries =
            transactions.findAllByUserIdAndOccurredOnBetweenOrderByOccurredOnDescCreatedAtDesc(
                userId, from, to);
        List<FinanceTransaction> cashEntries =
            transactions.findAllByUserIdAndOccurredOnLessThanEqual(userId, to);
        List<CashAdjustment> cashAdjustments =
            adjustments.findAllByUserIdAndAdjustedOnLessThanEqualOrderByAdjustedOnDescCreatedAtDesc(
                userId, to);
        Map<UUID, FinanceBucket> bucketMap = lookupBuckets(userId);
        Map<UUID, FinanceCategory> categoryMap = lookupCategories(userId);
        FinanceMonthPlan plan = plans.findByUserIdAndMonthStart(userId, from).orElse(null);
        Map<UUID, BigDecimal> planBudgets = plan == null ? Map.of() :
            budgets.findAllByPlanId(plan.getId()).stream().collect(
                LinkedHashMap::new, (map, item) -> map.put(item.getBucketId(), item.getAmount()),
                LinkedHashMap::putAll);

        BigDecimal income = sum(entries, FinanceTransactionType.INCOME);
        BigDecimal expenses = sum(entries, FinanceTransactionType.EXPENSE);
        BigDecimal refunds = sum(entries, FinanceTransactionType.REFUND);
        BigDecimal netExpenses = expenses.subtract(refunds).max(ZERO);
        BigDecimal savings = sum(entries, FinanceTransactionType.SAVINGS);
        BigDecimal available = income.subtract(netExpenses).subtract(savings);
        BigDecimal rate = income.signum() == 0 ? null :
            savings.multiply(new BigDecimal("100")).divide(income, 1, RoundingMode.HALF_UP);
        BigDecimal cash = cashBalance(cashEntries, cashAdjustments);

        List<BucketFlow> flows = bucketMap.values().stream()
            .sorted(Comparator.comparingInt(FinanceBucket::getPositionIndex))
            .map(bucket -> bucketFlow(bucket, entries, planBudgets.getOrDefault(
                bucket.getId(), ZERO))).toList();
        List<DailyFlow> rhythm = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            LocalDate day = date;
            List<FinanceTransaction> daily = entries.stream()
                .filter(entry -> entry.getOccurredOn().equals(day)).toList();
            rhythm.add(new DailyFlow(day, sum(daily, FinanceTransactionType.INCOME),
                sum(daily, FinanceTransactionType.EXPENSE)
                    .subtract(sum(daily, FinanceTransactionType.REFUND)).max(ZERO),
                sum(daily, FinanceTransactionType.SAVINGS)));
        }
        return new DashboardResponse(profiles.findById(userId).orElseThrow().getCurrencyCode(),
            from, new FinanceSummary(income, expenses, refunds, netExpenses, savings,
                available, rate, cash), plan == null ? null : planResponse(plan), flows, rhythm,
            entries.stream().map(entry -> transactionResponse(entry, bucketMap, categoryMap))
                .toList(),
            cashAdjustments.stream().map(this::adjustmentResponse).toList());
    }

    @Transactional
    public FinanceInsightsResponse insights(UUID userId, String requestedPeriod,
                                            LocalDate anchor) {
        ensureSetup(userId);
        String period = requestedPeriod == null
            ? "MONTH" : requestedPeriod.trim().toUpperCase(Locale.ROOT);
        if (!period.equals("MONTH") && !period.equals("YEAR")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Choose MONTH or YEAR for Financial Flow insights.");
        }
        LocalDate from = period.equals("YEAR")
            ? anchor.withDayOfYear(1) : anchor.withDayOfMonth(1);
        LocalDate to = period.equals("YEAR")
            ? anchor.withMonth(12).withDayOfMonth(31)
            : anchor.withDayOfMonth(anchor.lengthOfMonth());
        List<FinanceTransaction> entries =
            transactions.findAllByUserIdAndOccurredOnBetweenOrderByOccurredOnDescCreatedAtDesc(
                userId, from, to);
        Map<UUID, FinanceBucket> bucketMap = lookupBuckets(userId);

        BigDecimal income = sum(entries, FinanceTransactionType.INCOME);
        BigDecimal expenses = sum(entries, FinanceTransactionType.EXPENSE);
        BigDecimal refunds = sum(entries, FinanceTransactionType.REFUND);
        BigDecimal netExpenses = expenses.subtract(refunds).max(ZERO);
        BigDecimal savings = sum(entries, FinanceTransactionType.SAVINGS);
        BigDecimal available = income.subtract(netExpenses).subtract(savings);
        BigDecimal savingsRate = percentage(savings, income);
        BigDecimal cash = cashBalance(
            transactions.findAllByUserIdAndOccurredOnLessThanEqual(userId, to),
            adjustments.findAllByUserIdAndAdjustedOnLessThanEqualOrderByAdjustedOnDescCreatedAtDesc(
                userId, to));
        FinanceSummary summary = new FinanceSummary(
            income, expenses, refunds, netExpenses, savings, available, savingsRate, cash);

        List<FinanceInsightBucket> bucketInsights = bucketMap.values().stream()
            .sorted(Comparator.comparingInt(FinanceBucket::getPositionIndex))
            .map(bucket -> {
                BigDecimal net = netForBucket(bucket.getId(), entries);
                return new FinanceInsightBucket(
                    bucket.getId(), bucket.getSystemKey(), bucket.getName(),
                    bucket.getColorHex(), net, percentage(net, income),
                    percentage(net, netExpenses));
            }).toList();

        List<FinanceInsightPoint> timeline = new ArrayList<>();
        if (period.equals("YEAR")) {
            for (int month = 1; month <= 12; month++) {
                LocalDate pointFrom = from.withMonth(month);
                LocalDate pointTo = pointFrom.withDayOfMonth(pointFrom.lengthOfMonth());
                timeline.add(insightPoint(pointFrom,
                    pointFrom.getMonth().getDisplayName(
                        java.time.format.TextStyle.SHORT, Locale.ENGLISH),
                    entriesBetween(entries, pointFrom, pointTo), bucketMap));
            }
        } else {
            for (LocalDate pointFrom = from; !pointFrom.isAfter(to);
                 pointFrom = pointFrom.plusDays(7)) {
                LocalDate pointTo = pointFrom.plusDays(6).isAfter(to)
                    ? to : pointFrom.plusDays(6);
                timeline.add(insightPoint(pointFrom,
                    pointFrom.getDayOfMonth() + "–" + pointTo.getDayOfMonth(),
                    entriesBetween(entries, pointFrom, pointTo), bucketMap));
            }
        }
        return new FinanceInsightsResponse(
            profiles.findById(userId).orElseThrow().getCurrencyCode(),
            period, from, to, summary, bucketInsights, List.copyOf(timeline));
    }

    private FinanceInsightPoint insightPoint(
        LocalDate periodStart, String label, List<FinanceTransaction> entries,
        Map<UUID, FinanceBucket> bucketMap) {
        BigDecimal income = sum(entries, FinanceTransactionType.INCOME);
        BigDecimal savings = sum(entries, FinanceTransactionType.SAVINGS);
        BigDecimal netExpenses = sum(entries, FinanceTransactionType.EXPENSE)
            .subtract(sum(entries, FinanceTransactionType.REFUND)).max(ZERO);
        Map<String, BigDecimal> bySystemKey = new HashMap<>();
        bucketMap.values().forEach(bucket ->
            bySystemKey.put(bucket.getSystemKey(), netForBucket(bucket.getId(), entries)));
        return new FinanceInsightPoint(
            periodStart, label, income, savings,
            bySystemKey.getOrDefault("NEEDS", ZERO),
            bySystemKey.getOrDefault("WANTS", ZERO),
            bySystemKey.getOrDefault("EXPERIENCES", ZERO),
            bySystemKey.getOrDefault("UNEXPECTED", ZERO),
            money(income.subtract(netExpenses).subtract(savings)));
    }

    private List<FinanceTransaction> entriesBetween(
        List<FinanceTransaction> entries, LocalDate from, LocalDate to) {
        return entries.stream().filter(entry ->
            !entry.getOccurredOn().isBefore(from) && !entry.getOccurredOn().isAfter(to)).toList();
    }

    private BigDecimal netForBucket(UUID bucketId, List<FinanceTransaction> entries) {
        BigDecimal spent = entries.stream()
            .filter(entry -> bucketId.equals(entry.getBucketId())
                && entry.getType() == FinanceTransactionType.EXPENSE)
            .map(FinanceTransaction::getAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal refunded = entries.stream()
            .filter(entry -> bucketId.equals(entry.getBucketId())
                && entry.getType() == FinanceTransactionType.REFUND)
            .map(FinanceTransaction::getAmount).reduce(ZERO, BigDecimal::add);
        return money(spent.subtract(refunded).max(ZERO));
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal base) {
        return base.signum() == 0 ? null
            : value.multiply(new BigDecimal("100"))
                .divide(base, 1, RoundingMode.HALF_UP);
    }

    private void ensureSetup(UUID userId) {
        profiles.findById(userId).orElseGet(() ->
            profiles.save(new FinanceProfile(userId, "INR")));
        if (!buckets.findAllByUserIdOrderByPositionIndexAsc(userId).isEmpty()) return;
        buckets.saveAll(List.of(
            new FinanceBucket(userId, "NEEDS", "Needs", "#3D7A63", "home", 0),
            new FinanceBucket(userId, "WANTS", "Wants", "#C98268", "spark", 1),
            new FinanceBucket(userId, "EXPERIENCES", "Experiences", "#547CA0", "journey", 2),
            new FinanceBucket(userId, "UNEXPECTED", "Unexpected", "#A87943", "umbrella", 3)
        ));
    }

    private void apply(UUID userId, FinanceTransaction transaction, TransactionRequest request) {
        UUID bucketId = request.bucketId();
        UUID categoryId = request.categoryId();
        if (request.type() == FinanceTransactionType.EXPENSE
            || request.type() == FinanceTransactionType.REFUND) {
            if (bucketId == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Choose a Kakeibo bucket for an expense or refund.");
            }
            requireBucket(userId, bucketId);
            if (categoryId != null) {
                FinanceCategory category = categories.findByIdAndUserId(categoryId, userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Choose one of your financial categories."));
                if (!category.getBucketId().equals(bucketId)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                        "The category must belong to the selected bucket.");
                }
            }
        } else {
            bucketId = null;
            categoryId = null;
        }
        if (request.type() == FinanceTransactionType.TRANSFER
            && request.transferDirection() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Choose whether this transfer moved cash in or out.");
        }
        if (request.type() != FinanceTransactionType.TRANSFER
            && request.transferDirection() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Cash transfer direction is only used for transfers.");
        }
        if (request.recurringUntil() != null && request.recurringFrequency() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Choose how often this recurring entry repeats.");
        }
        if (request.recurringUntil() != null
            && request.recurringUntil().isBefore(request.occurredOn())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "A recurring end date cannot be before the first entry.");
        }
        transaction.update(request.type(), bucketId, categoryId, request.title().trim(),
            money(request.amount()), request.occurredOn(), request.paymentMode(),
            request.transferDirection(), trim(request.incomeSource()), trim(request.note()),
            request.recurringFrequency(), request.recurringUntil());
    }

    private BigDecimal cashBalance(List<FinanceTransaction> entries,
                                   List<CashAdjustment> cashAdjustments) {
        BigDecimal balance = cashAdjustments.stream().map(CashAdjustment::getAmount)
            .reduce(ZERO, BigDecimal::add);
        for (FinanceTransaction entry : entries) {
            if (entry.getType() == FinanceTransactionType.TRANSFER) {
                if (entry.getTransferDirection() == TransferDirection.CASH_IN) {
                    balance = balance.add(entry.getAmount());
                } else if (entry.getTransferDirection() == TransferDirection.CASH_OUT) {
                    balance = balance.subtract(entry.getAmount());
                }
            } else if (entry.getPaymentMode() == PaymentMode.CASH) {
                balance = switch (entry.getType()) {
                    case INCOME, REFUND -> balance.add(entry.getAmount());
                    case EXPENSE, SAVINGS -> balance.subtract(entry.getAmount());
                    case TRANSFER -> balance;
                };
            }
        }
        return money(balance);
    }

    private BucketFlow bucketFlow(FinanceBucket bucket, List<FinanceTransaction> entries,
                                  BigDecimal budget) {
        BigDecimal spent = entries.stream()
            .filter(entry -> bucket.getId().equals(entry.getBucketId())
                && entry.getType() == FinanceTransactionType.EXPENSE)
            .map(FinanceTransaction::getAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal refunded = entries.stream()
            .filter(entry -> bucket.getId().equals(entry.getBucketId())
                && entry.getType() == FinanceTransactionType.REFUND)
            .map(FinanceTransaction::getAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal net = spent.subtract(refunded).max(ZERO);
        BigDecimal remaining = budget.signum() == 0 ? ZERO : budget.subtract(net);
        BigDecimal used = budget.signum() == 0 ? null :
            net.multiply(new BigDecimal("100")).divide(budget, 1, RoundingMode.HALF_UP);
        return new BucketFlow(bucket.getId(), bucket.getSystemKey(), bucket.getName(),
            bucket.getColorHex(), bucket.getIconKey(), money(budget), money(spent),
            money(refunded), money(net), money(remaining), used);
    }

    private BigDecimal sum(List<FinanceTransaction> entries, FinanceTransactionType type) {
        return money(entries.stream().filter(entry -> entry.getType() == type)
            .map(FinanceTransaction::getAmount).reduce(ZERO, BigDecimal::add));
    }

    private MonthPlanResponse planResponse(FinanceMonthPlan plan) {
        Map<UUID, BigDecimal> bucketBudgets = new LinkedHashMap<>();
        budgets.findAllByPlanId(plan.getId()).forEach(
            budget -> bucketBudgets.put(budget.getBucketId(), budget.getAmount()));
        return new MonthPlanResponse(plan.getMonthStart(), plan.getExpectedIncome(),
            plan.getSavingsTarget(), plan.getIntention(), plan.getWentWell(),
            plan.getLearned(), plan.getNextMonthChange(), bucketBudgets);
    }

    private Map<UUID, FinanceBucket> lookupBuckets(UUID userId) {
        Map<UUID, FinanceBucket> map = new LinkedHashMap<>();
        buckets.findAllByUserIdOrderByPositionIndexAsc(userId)
            .forEach(bucket -> map.put(bucket.getId(), bucket));
        return map;
    }
    private Map<UUID, FinanceCategory> lookupCategories(UUID userId) {
        Map<UUID, FinanceCategory> map = new HashMap<>();
        categories.findAllByUserIdOrderByPositionIndexAsc(userId)
            .forEach(category -> map.put(category.getId(), category));
        return map;
    }
    private FinanceBucket requireBucket(UUID userId, UUID id) {
        return buckets.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Financial bucket was not found."));
    }
    private BucketResponse bucketResponse(FinanceBucket bucket) {
        return new BucketResponse(bucket.getId(), bucket.getSystemKey(), bucket.getName(),
            bucket.getColorHex(), bucket.getIconKey(), bucket.getPositionIndex(),
            bucket.isActive());
    }
    private CategoryResponse categoryResponse(FinanceCategory category) {
        return new CategoryResponse(category.getId(), category.getBucketId(), category.getName(),
            category.getPositionIndex(), category.isActive());
    }
    private TransactionResponse transactionResponse(FinanceTransaction entry,
                                                    Map<UUID, FinanceBucket> bucketMap,
                                                    Map<UUID, FinanceCategory> categoryMap) {
        FinanceBucket bucket = bucketMap.get(entry.getBucketId());
        FinanceCategory category = categoryMap.get(entry.getCategoryId());
        return new TransactionResponse(entry.getId(), entry.getType(), entry.getBucketId(),
            bucket == null ? null : bucket.getName(), entry.getCategoryId(),
            category == null ? null : category.getName(), entry.getTitle(), entry.getAmount(),
            entry.getOccurredOn(), entry.getPaymentMode(), entry.getTransferDirection(),
            entry.getIncomeSource(), entry.getNote(), entry.getRecurringFrequency(),
            entry.getRecurringUntil());
    }
    private CashAdjustmentResponse adjustmentResponse(CashAdjustment adjustment) {
        return new CashAdjustmentResponse(adjustment.getId(), adjustment.getAmount(),
            adjustment.getAdjustedOn(), adjustment.getReason(), adjustment.getAdjustmentKind());
    }
    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
