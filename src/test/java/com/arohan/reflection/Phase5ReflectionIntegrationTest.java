package com.arohan.reflection;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Phase5ReflectionIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void reflectionTagsFiltersAndExplainableSignalsWorkTogether() throws Exception {
        String token = register();
        String tagId = createTag(token, "learning");
        String entryId = createReflection(token, tagId);

        mockMvc.perform(get("/api/v1/reflections")
                .param("query", "gentler")
                .param("pinned", "true")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(entryId))
            .andExpect(jsonPath("$[0].tags[0].name").value("learning"))
            .andExpect(jsonPath("$[0].energyScore").value(3));

        mockMvc.perform(get("/api/v1/growth-signals")
                .param("from", LocalDate.now().minusDays(29).toString())
                .param("to", LocalDate.now().toString())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signals.length()").value(5))
            .andExpect(jsonPath("$.signals[0].key").value("natural-time"))
            .andExpect(jsonPath("$.signals[2].key").value("improving-area"))
            .andExpect(jsonPath("$.signals[3].method").exists())
            .andExpect(jsonPath("$.boundaryNote").exists());

        mockMvc.perform(delete("/api/v1/reflections/tags/" + tagId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict());
    }

    @Test
    void reflectionsRemainPrivateAndRequireMeaningfulInput() throws Exception {
        String owner = register();
        String other = register();
        String entryId = createReflection(owner, null);

        mockMvc.perform(delete("/api/v1/reflections/" + entryId)
                .header("Authorization", "Bearer " + other))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/reflections")
                .header("Authorization", "Bearer " + owner)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "entryType":"DAILY_NOTE","entryDate":"%s",
                      "title":"","content":"","pinned":false,"tagIds":[]
                    }
                    """.formatted(LocalDate.now())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Add a thought, a mood, or an energy check-in before saving."));
    }

    private String createReflection(String token, String tagId) throws Exception {
        String tags = tagId == null ? "[]" : "[\"" + tagId + "\"]";
        String body = mockMvc.perform(post("/api/v1/reflections")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "entryType":"DAILY_NOTE","title":"A small return",
                      "content":"A gentler beginning helped.","entryDate":"%s",
                      "moodScore":4,"energyScore":3,"pinned":true,"tagIds":%s
                    }
                    """.formatted(LocalDate.now(), tags)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String createTag(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/reflections/tags")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"%s","colorHex":"#8A6B91"}
                    """.formatted(name)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String register() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":"Phase Five","email":"phase5-%s@example.com",
                     "password":"a-strong-passphrase"}
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("accessToken").asText();
    }
}
