package com.arohan.finance;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Phase4FinanceIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void dashboardKeepsRefundSavingsTransferAndCashMathDistinct() throws Exception {
        String token = register();
        JsonNode setup = json(mockMvc.perform(get("/api/v1/financial-flow/setup")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.buckets", hasSize(4)))
            .andReturn().getResponse().getContentAsString());
        String needsId = setup.path("buckets").get(0).path("id").asText();
        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.from(today);

        transaction(token, "INCOME", null, "Salary", "50000.00", today, "BANK", null);
        transaction(token, "EXPENSE", needsId, "Rent", "12000.00", today, "BANK", null);
        transaction(token, "REFUND", needsId, "Rent correction", "2000.00", today, "BANK", null);
        transaction(token, "SAVINGS", null, "Emergency fund", "10000.00", today, "BANK", null);
        transaction(token, "TRANSFER", null, "ATM withdrawal", "3000.00", today, "BANK",
            "CASH_IN");
        transaction(token, "EXPENSE", needsId, "Groceries", "500.00", today, "CASH", null);

        mockMvc.perform(post("/api/v1/financial-flow/cash-adjustments")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amount":1000.00,"adjustedOn":"%s","reason":"Opening wallet",
                     "adjustmentKind":"OPENING"}
                    """.formatted(today)))
            .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/financial-flow/months/" + month + "/plan")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"expectedIncome":50000.00,"savingsTarget":12000.00,
                     "intention":"Spend with attention","wentWell":"","learned":"",
                     "nextMonthChange":"",
                     "bucketBudgets":[{"bucketId":"%s","amount":15000.00}]}
                    """.formatted(needsId)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/financial-flow/dashboard")
                .param("month", month.toString())
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.income").value(50000.00))
            .andExpect(jsonPath("$.summary.expenses").value(12500.00))
            .andExpect(jsonPath("$.summary.refunds").value(2000.00))
            .andExpect(jsonPath("$.summary.netExpenses").value(10500.00))
            .andExpect(jsonPath("$.summary.savings").value(10000.00))
            .andExpect(jsonPath("$.summary.available").value(29500.00))
            .andExpect(jsonPath("$.summary.savingsRatePercent").value(20.0))
            .andExpect(jsonPath("$.summary.cashBalance").value(3500.00))
            .andExpect(jsonPath("$.bucketFlows[0].netSpent").value(10500.00))
            .andExpect(jsonPath("$.bucketFlows[0].remaining").value(4500.00))
            .andExpect(jsonPath("$.transactions", hasSize(6)));

        mockMvc.perform(get("/api/v1/financial-flow/insights")
                .param("period", "MONTH")
                .param("anchor", today.toString())
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.period").value("MONTH"))
            .andExpect(jsonPath("$.summary.income").value(50000.00))
            .andExpect(jsonPath("$.summary.netExpenses").value(10500.00))
            .andExpect(jsonPath("$.summary.savings").value(10000.00))
            .andExpect(jsonPath("$.summary.savingsRatePercent").value(20.0))
            .andExpect(jsonPath("$.buckets[0].netSpent").value(10500.00))
            .andExpect(jsonPath("$.buckets[0].percentOfIncome").value(21.0))
            .andExpect(jsonPath("$.timeline").isArray());
    }

    @Test
    void expenseRequiresBucketAndTransferRequiresDirection() throws Exception {
        String token = register();
        mockMvc.perform(get("/api/v1/financial-flow/setup")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk());
        LocalDate today = LocalDate.now();
        mockMvc.perform(post("/api/v1/financial-flow/transactions")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type":"EXPENSE","title":"Unknown","amount":10.00,
                     "occurredOn":"%s","paymentMode":"CASH"}
                    """.formatted(today)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Choose a Kakeibo bucket for an expense or refund."));
        mockMvc.perform(post("/api/v1/financial-flow/transactions")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type":"TRANSFER","title":"Move cash","amount":10.00,
                     "occurredOn":"%s","paymentMode":"BANK"}
                    """.formatted(today)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Choose whether this transfer moved cash in or out."));
    }

    @Test
    void usersCannotSpendFromAnotherUsersBucketAndCurrencyLocksAfterFirstEntry()
        throws Exception {
        String firstToken = register();
        String secondToken = register();
        JsonNode firstSetup = json(mockMvc.perform(get("/api/v1/financial-flow/setup")
                .header("Authorization", bearer(firstToken)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mockMvc.perform(get("/api/v1/financial-flow/setup")
                .header("Authorization", bearer(secondToken)))
            .andExpect(status().isOk());
        String foreignBucket = firstSetup.path("buckets").get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/financial-flow/transactions")
                .header("Authorization", bearer(secondToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type":"EXPENSE","bucketId":"%s","title":"Not mine","amount":10.00,
                     "occurredOn":"%s","paymentMode":"CASH"}
                    """.formatted(foreignBucket, LocalDate.now())))
            .andExpect(status().isNotFound());

        transaction(firstToken, "INCOME", null, "First income", "100.00",
            LocalDate.now(), "BANK", null);
        mockMvc.perform(patch("/api/v1/financial-flow/profile")
                .header("Authorization", bearer(firstToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"USD\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(
                "Currency cannot be changed after money has been recorded."));
    }

    private void transaction(String token, String type, String bucketId, String title,
                             String amount, LocalDate date, String paymentMode,
                             String transferDirection) throws Exception {
        String body = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("type", type);
            if (bucketId != null) put("bucketId", bucketId);
            put("title", title);
            put("amount", amount);
            put("occurredOn", date.toString());
            put("paymentMode", paymentMode);
            if (transferDirection != null) put("transferDirection", transferDirection);
        }});
        mockMvc.perform(post("/api/v1/financial-flow/transactions")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
    }

    private String register() throws Exception {
        String email = "finance-" + System.nanoTime() + "@example.com";
        String body = """
            {"displayName":"Finance Tester","email":"%s","password":"StrongPass123!"}
            """.formatted(email);
        return json(mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
            .path("accessToken").asText();
    }
    private JsonNode json(String content) throws Exception {
        return objectMapper.readTree(content);
    }
    private String bearer(String token) { return "Bearer " + token; }
}
