package com.arohan.tracking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Phase3TrackingIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void todayCueCompletionAndStudioUseEligibleOpportunities() throws Exception {
        String token = register();
        updateTimeZone(token, "Asia/Kolkata");
        String areaId = createArea(token);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate start = today.minusDays(3);
        String habitId = createDailyHabit(token, areaId, start);
        jdbc.update("update growth_habit set tracking_enabled_from=? where id=?",
            start, UUID.fromString(habitId));

        mockMvc.perform(post("/api/v1/tracking/habits/" + habitId + "/cue-start")
                .param("date", start.toString())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cueStartedAt").exists());

        record(token, habitId, start, "COMPLETED", 4, "The mat was already visible.");
        record(token, habitId, today.minusDays(1), "COMPLETED", 5,
            "Returning felt easier than restarting from zero.");

        mockMvc.perform(get("/api/v1/tracking/today")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.date").value(today.toString()))
            .andExpect(jsonPath("$.timeZone").value("Asia/Kolkata"))
            .andExpect(jsonPath("$.remainingCount").value(1))
            .andExpect(jsonPath("$.habits[0].cueNote").value("Place the mat beside the bed"));

        mockMvc.perform(get("/api/v1/growth-studio")
                .param("from", start.toString())
                .param("to", today.toString())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.counts.eligible").value(4))
            .andExpect(jsonPath("$.counts.completed").value(2))
            .andExpect(jsonPath("$.counts.missed").value(1))
            .andExpect(jsonPath("$.counts.consistencyPercent").value(50.0))
            .andExpect(jsonPath("$.signals.length()").value(3))
            .andExpect(jsonPath("$.signals[0].label").value("Rhythm met"))
            .andExpect(jsonPath("$.signals[2].key").value("RECOVERY"))
            .andExpect(jsonPath("$.signals[2].value").value(100.0))
            .andExpect(jsonPath("$.signals[2].ready").value(false))
            .andExpect(jsonPath("$.cueFlow.cueStarts").value(1));
    }

    @Test
    void trackingIsOwnershipSafe() throws Exception {
        String owner = register();
        String other = register();
        String areaId = createArea(owner);
        LocalDate today = LocalDate.now();
        String habitId = createDailyHabit(owner, areaId, today);

        mockMvc.perform(post("/api/v1/tracking/habits/" + habitId + "/cue-start")
                .param("date", today.toString())
                .header("Authorization", "Bearer " + other))
            .andExpect(status().isNotFound());
    }

    private void record(String token, String habitId, LocalDate date, String status,
                        int quality, String reflection) throws Exception {
        mockMvc.perform(put("/api/v1/tracking/habits/" + habitId + "/practice")
                .param("date", date.toString())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"%s","qualityRating":%d,"reflection":"%s",
                     "frictionNote":""}
                    """.formatted(status, quality, reflection)))
            .andExpect(status().isOk());
    }

    private String createArea(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/life-areas")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"parentId":null,"name":"Physical vitality","description":"",
                     "colorHex":"#D87867","iconKey":"vitality","backgroundKey":"sunrise",
                     "backgroundImageUrl":"","desiredImportance":4,"positionIndex":0}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String createDailyHabit(String token, String areaId, LocalDate start)
        throws Exception {
        String body = mockMvc.perform(post("/api/v1/growth-habits")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "kind":"GROWTH_HABIT","lifeAreaId":"%s","name":"Morning mobility",
                      "purpose":"Move with ease","trackingMethod":"CHECKBOX",
                      "cueNote":"Place the mat beside the bed",
                      "twoMinuteStarter":"Take three slow stretches",
                      "fallbackPlan":"If morning passes, stretch before lunch.",
                      "positionIndex":0,
                      "schedule":{"type":"DAILY","startDate":"%s","weekdays":[]}
                    }
                    """.formatted(areaId, start)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String register() throws Exception {
        String email = "phase3-" + UUID.randomUUID() + "@example.com";
        String body = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":"Phase Three","email":"%s",
                     "password":"a-strong-passphrase"}
                    """.formatted(email)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("accessToken").asText();
    }

    private void updateTimeZone(String token, String zone) throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "displayName":"Phase Three","timeZone":"%s","locale":"en-IN",
                      "themePreference":"SYSTEM","weekStart":"MONDAY",
                      "dateFormat":"AUTO","timeFormat":"SYSTEM",
                      "reducedMotion":false,"enhancedContrast":false,
                      "onboardingComplete":true,"starterTemplateKeys":[]
                    }
                    """.formatted(zone)))
            .andExpect(status().isOk());
    }
}
