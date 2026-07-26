package com.arohan.habit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Phase2FlowIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void lifeAreaAndCueFirstHabitFlowPreservesOwnershipAndState() throws Exception {
        String ownerToken = register("phase2-owner-" + UUID.randomUUID() + "@example.com");
        String otherToken = register("phase2-other-" + UUID.randomUUID() + "@example.com");

        String areaBody = mockMvc.perform(post("/api/v1/life-areas")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentId":null,
                      "name":"Physical vitality",
                      "description":"Feel awake and capable",
                      "colorHex":"",
                      "iconKey":"",
                      "backgroundKey":"",
                      "backgroundImageUrl":"",
                      "desiredImportance":4,
                      "positionIndex":0
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.iconKey").value("vitality"))
            .andReturn().getResponse().getContentAsString();
        String areaId = objectMapper.readTree(areaBody).get("id").asText();

        String habitBody = mockMvc.perform(post("/api/v1/growth-habits")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "kind":"GROWTH_HABIT",
                      "lifeAreaId":"%s",
                      "name":"Morning mobility",
                      "purpose":"I am becoming someone who moves with ease.",
                      "trackingMethod":"DURATION",
                      "targetValue":10,
                      "targetUnit":"minutes",
                      "cueNote":"Put the mat beside the bed",
                      "twoMinuteStarter":"Take three slow stretches",
                      "preferredTime":"07:00",
                      "preferredPlace":"Bedroom",
                      "precedingActivity":"After drinking water",
                      "situation":"",
                      "fallbackPlan":"If morning passes, then stretch before lunch.",
                      "positionIndex":0,
                      "schedule":{
                        "type":"SELECTED_WEEKDAYS",
                        "startDate":"2026-07-23",
                        "weekdays":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY",
                                    "FRIDAY","SATURDAY","SUNDAY"],
                        "intervalDays":null,
                        "targetCount":null,
                        "dueDate":null,
                        "customDescription":""
                      }
                    }
                    """.formatted(areaId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cueNote").value("Put the mat beside the bed"))
            .andExpect(jsonPath("$.schedule.weekdays.length()").value(7))
            .andReturn().getResponse().getContentAsString();
        String habitId = objectMapper.readTree(habitBody).get("id").asText();

        mockMvc.perform(get("/api/v1/growth-habits/" + habitId)
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/growth-habits/" + habitId + "/pause")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(patch("/api/v1/growth-habits/" + habitId + "/restart")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/v1/life-areas/" + areaId + "/archive")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/growth-habits/" + habitId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(delete("/api/v1/growth-habits/" + habitId)
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/growth-habits/" + habitId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/growth-habits/" + habitId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectsScheduleThatCannotExpressItsRhythm() throws Exception {
        String token = register("phase2-invalid-" + UUID.randomUUID() + "@example.com");
        String areaBody = mockMvc.perform(post("/api/v1/life-areas")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"parentId":null,"name":"Learning","description":"",
                     "colorHex":"#397E9A","iconKey":"learning","backgroundKey":"open-sky",
                     "backgroundImageUrl":"","desiredImportance":3,"positionIndex":0}
                    """))
            .andReturn().getResponse().getContentAsString();
        String areaId = objectMapper.readTree(areaBody).get("id").asText();

        mockMvc.perform(post("/api/v1/growth-habits")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "kind":"GROWTH_HABIT","lifeAreaId":"%s","name":"Read",
                      "purpose":"Become a thoughtful learner","trackingMethod":"CHECKBOX",
                      "cueNote":"Open the book","twoMinuteStarter":"Read one paragraph",
                      "fallbackPlan":"If evening is busy, read after lunch.","positionIndex":0,
                      "schedule":{"type":"SELECTED_WEEKDAYS","startDate":"2026-07-23",
                      "weekdays":[]}
                    }
                    """.formatted(areaId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Choose at least one weekday."));
    }

    @Test
    void repeatedCreateRequestReturnsTheSameHabitWithoutCreatingADuplicate() throws Exception {
        String token = register("phase2-idempotent-" + UUID.randomUUID() + "@example.com");
        String areaBody = mockMvc.perform(post("/api/v1/life-areas")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"parentId":null,"name":"Learning","description":"",
                     "colorHex":"#397E9A","iconKey":"learning","backgroundKey":"open-sky",
                     "backgroundImageUrl":"","desiredImportance":3,"positionIndex":0}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String areaId = objectMapper.readTree(areaBody).get("id").asText();
        String creationId = UUID.randomUUID().toString();
        String request = """
            {
              "clientRequestId":"%s",
              "kind":"GROWTH_HABIT","lifeAreaId":"%s","name":"Read",
              "purpose":"Become a thoughtful learner","trackingMethod":"CHECKBOX",
              "cueNote":"Open the book","twoMinuteStarter":"Read one paragraph",
              "fallbackPlan":"If evening is busy, read after lunch.","positionIndex":0,
              "schedule":{"type":"DAILY","startDate":"2026-07-26","weekdays":[]}
            }
            """.formatted(creationId, areaId);

        String first = mockMvc.perform(post("/api/v1/growth-habits")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/v1/growth-habits")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(
            objectMapper.readTree(second).get("id").asText())
            .isEqualTo(objectMapper.readTree(first).get("id").asText());
        mockMvc.perform(get("/api/v1/growth-habits")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    private String register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":"Phase Two","email":"%s","password":"a-strong-passphrase"}
                    """.formatted(email)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("accessToken").asText();
    }
}
