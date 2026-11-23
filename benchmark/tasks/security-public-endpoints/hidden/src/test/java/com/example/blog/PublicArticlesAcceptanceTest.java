package com.example.blog;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class PublicArticlesAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void anonymousReadersSeeArticles() throws Exception {
        var id = new Api(mockMvc).with(user("editor")).create("/articles", "{\"title\": \"Hello\", \"body\": \"World\"}");

        var anonymous = new Api(mockMvc);
        anonymous.get("/articles/{id}", id).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Hello"));
        anonymous.get("/articles").andExpect(status().isOk());
    }

    @Test
    void writingStillRequiresAuthentication() throws Exception {
        new Api(mockMvc).post("/articles", "{\"title\": \"Spam\", \"body\": \"Spam\"}")
                .andExpect(status().isUnauthorized());
    }
}
