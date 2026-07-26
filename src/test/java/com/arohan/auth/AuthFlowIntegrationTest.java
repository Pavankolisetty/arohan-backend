package com.arohan.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arohan.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired TestRestTemplate restTemplate;

    @Test
    void runningServerPublishesArohanHealth() {
        @SuppressWarnings("unchecked")
        Map<String, Object> health = restTemplate.getForObject("/api/v1/health", Map.class);
        assertThat(health).containsEntry("status", "UP").containsEntry("service", "arohan-api");
    }

    @Test
    void registrationHashesPasswordAndTokenAccessesCurrentUser() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":"Pavan","email":"PAVAN@example.com","password":"a-strong-passphrase"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.email").value("pavan@example.com"))
            .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        String token = json.get("accessToken").asText();
        var stored = users.findByEmail("pavan@example.com").orElseThrow();
        assertThat(stored.getPasswordHash()).doesNotContain("a-strong-passphrase");
        assertThat(passwordEncoder.matches("a-strong-passphrase", stored.getPasswordHash())).isTrue();

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Pavan"));

        mockMvc.perform(patch("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "displayName":"Pavan",
                      "timeZone":"Asia/Kolkata",
                      "locale":"en-IN",
                      "themePreference":"DARK",
                      "weekStart":"MONDAY",
                      "dateFormat":"DAY_FIRST",
                      "timeFormat":"TWENTY_FOUR_HOUR",
                      "reducedMotion":true,
                      "enhancedContrast":true,
                      "onboardingComplete":true,
                      "starterTemplateKeys":["LEARNING"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dateFormat").value("DAY_FIRST"))
            .andExpect(jsonPath("$.timeFormat").value("TWENTY_FOUR_HOUR"))
            .andExpect(jsonPath("$.reducedMotion").value(true))
            .andExpect(jsonPath("$.enhancedContrast").value(true));
    }

    @Test
    void protectedRouteRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }
}
