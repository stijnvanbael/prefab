package com.example.legal;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ContractAuditAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void recordsCreatorAndLastModifier() throws Exception {
        var ada = new Api(mockMvc).with(user("ada"));
        var grace = new Api(mockMvc).with(user("grace"));
        var before = Instant.now().minusSeconds(5);
        var id = ada.create("/contracts", "{\"title\": \"NDA\"}");

        var created = ada.get("/contracts/{id}", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.audit.createdBy").value("ada"))
                .andExpect(jsonPath("$.audit.lastModifiedBy").value("ada"));
        var createdAt = Instant.parse(Api.<String>read(created, "$.audit.createdAt"));
        assertThat(createdAt).isAfter(before);

        Thread.sleep(20);
        grace.put("/contracts/{id}", "{\"title\": \"Mutual NDA\"}", id).andExpect(status().isOk());

        var updated = grace.get("/contracts/{id}", id)
                .andExpect(jsonPath("$.title").value("Mutual NDA"))
                .andExpect(jsonPath("$.audit.createdBy").value("ada"))
                .andExpect(jsonPath("$.audit.lastModifiedBy").value("grace"));
        assertThat(Instant.parse(Api.<String>read(updated, "$.audit.createdAt"))).isEqualTo(createdAt);
        assertThat(Instant.parse(Api.<String>read(updated, "$.audit.lastModifiedAt"))).isAfter(createdAt);
    }
}
