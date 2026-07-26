package com.arohan.finance;

import static com.arohan.finance.FinanceDtos.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/financial-flow")
public class FinanceController {
    private final FinanceService service;
    public FinanceController(FinanceService service) { this.service = service; }

    @GetMapping("/setup")
    SetupResponse setup(@AuthenticationPrincipal Jwt jwt) {
        return service.setup(userId(jwt));
    }
    @PatchMapping("/profile")
    SetupResponse changeCurrency(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return service.changeCurrency(userId(jwt), request);
    }
    @PutMapping("/buckets/{id}")
    BucketResponse updateBucket(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody UpdateBucketRequest request) {
        return service.updateBucket(userId(jwt), id, request);
    }
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    CategoryResponse createCategory(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody CategoryRequest request) {
        return service.createCategory(userId(jwt), request);
    }
    @PutMapping("/categories/{id}")
    CategoryResponse updateCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                    @Valid @RequestBody CategoryRequest request) {
        return service.updateCategory(userId(jwt), id, request);
    }
    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse createTransaction(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody TransactionRequest request) {
        return service.createTransaction(userId(jwt), request);
    }
    @PutMapping("/transactions/{id}")
    TransactionResponse updateTransaction(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable UUID id,
                                          @Valid @RequestBody TransactionRequest request) {
        return service.updateTransaction(userId(jwt), id, request);
    }
    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTransaction(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.deleteTransaction(userId(jwt), id);
    }
    @PostMapping("/cash-adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    CashAdjustmentResponse adjustCash(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody CashAdjustmentRequest request) {
        return service.adjustCash(userId(jwt), request);
    }
    @PutMapping("/months/{month}/plan")
    MonthPlanResponse savePlan(@AuthenticationPrincipal Jwt jwt, @PathVariable YearMonth month,
                               @Valid @RequestBody MonthPlanRequest request) {
        return service.savePlan(userId(jwt), month, request);
    }
    @GetMapping("/dashboard")
    DashboardResponse dashboard(@AuthenticationPrincipal Jwt jwt,
                                @RequestParam(required = false) YearMonth month) {
        return service.dashboard(userId(jwt), month == null ? YearMonth.now() : month);
    }
    @GetMapping("/insights")
    FinanceInsightsResponse insights(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(defaultValue = "MONTH") String period,
                                     @RequestParam(required = false) LocalDate anchor) {
        return service.insights(userId(jwt), period, anchor == null ? LocalDate.now() : anchor);
    }
    private UUID userId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
