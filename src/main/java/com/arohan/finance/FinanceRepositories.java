package com.arohan.finance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FinanceProfileRepository extends JpaRepository<FinanceProfile, UUID> {}

interface FinanceBucketRepository extends JpaRepository<FinanceBucket, UUID> {
    List<FinanceBucket> findAllByUserIdOrderByPositionIndexAsc(UUID userId);
    Optional<FinanceBucket> findByIdAndUserId(UUID id, UUID userId);
}

interface FinanceCategoryRepository extends JpaRepository<FinanceCategory, UUID> {
    List<FinanceCategory> findAllByUserIdOrderByPositionIndexAsc(UUID userId);
    Optional<FinanceCategory> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndBucketIdAndNameIgnoreCase(UUID userId, UUID bucketId, String name);
}

interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, UUID> {
    List<FinanceTransaction> findAllByUserIdAndOccurredOnBetweenOrderByOccurredOnDescCreatedAtDesc(
        UUID userId, LocalDate from, LocalDate to);
    List<FinanceTransaction> findAllByUserIdAndOccurredOnLessThanEqual(
        UUID userId, LocalDate to);
    Optional<FinanceTransaction> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserId(UUID userId);
}

interface CashAdjustmentRepository extends JpaRepository<CashAdjustment, UUID> {
    List<CashAdjustment> findAllByUserIdAndAdjustedOnLessThanEqualOrderByAdjustedOnDescCreatedAtDesc(
        UUID userId, LocalDate to);
}

interface FinanceMonthPlanRepository extends JpaRepository<FinanceMonthPlan, UUID> {
    Optional<FinanceMonthPlan> findByUserIdAndMonthStart(UUID userId, LocalDate monthStart);
}

interface FinanceBucketBudgetRepository extends JpaRepository<FinanceBucketBudget, UUID> {
    List<FinanceBucketBudget> findAllByPlanId(UUID planId);
    Optional<FinanceBucketBudget> findByPlanIdAndBucketId(UUID planId, UUID bucketId);
}
