package com.arohan.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arohan.user.UserRepository;
import com.arohan.habit.GrowthHabitRepository;
import com.arohan.habit.HabitScheduleRepository;
import com.arohan.lifearea.LifeAreaRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired LifeAreaRepository lifeAreas;
    @Autowired GrowthHabitRepository growthHabits;
    @Autowired HabitScheduleRepository habitSchedules;
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

        var learningArea = lifeAreas
            .findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(stored.getId())
            .stream()
            .filter(area -> area.getName().equals("Learning"))
            .findFirst()
            .orElseThrow();
        var starterHabits = growthHabits
            .findAllByUserIdAndLifeAreaId(stored.getId(), learningArea.getId());
        assertThat(starterHabits).hasSize(1);
        assertThat(starterHabits.get(0).getName()).isEqualTo("Read for ten minutes");
        assertThat(habitSchedules.findByHabitId(starterHabits.get(0).getId())
            .orElseThrow().getWeekdays())
            .contains("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
            .doesNotContain("SATURDAY", "SUNDAY");

        // Saving onboarding preferences again must not duplicate the starter setup.
        mockMvc.perform(patch("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "displayName":"Pavan","timeZone":"Asia/Kolkata","locale":"en-IN",
                      "themePreference":"DARK","weekStart":"MONDAY",
                      "dateFormat":"DAY_FIRST","timeFormat":"TWENTY_FOUR_HOUR",
                      "reducedMotion":true,"enhancedContrast":true,
                      "onboardingComplete":true,"starterTemplateKeys":["LEARNING"]
                    }
                    """))
            .andExpect(status().isOk());
        assertThat(growthHabits.findAllByUserIdAndLifeAreaId(
            stored.getId(), learningArea.getId())).hasSize(1);
    }

    @Test
    void protectedRouteRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void vercelProjectPreviewCanCallAuthenticationApi() throws Exception {
        String previewOrigin =
            "https://arohan-frontend-jxtoy07r1-pavankolisettys-projects.vercel.app";

        mockMvc.perform(options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, previewOrigin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, previewOrigin));
    }
}
