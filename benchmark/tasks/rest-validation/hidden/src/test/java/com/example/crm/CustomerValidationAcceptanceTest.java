package com.example.crm;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CustomerValidationAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void validCustomerIsCreated() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/customers", """
                {"name": "Ada Lovelace", "email": "ada@example.com"}
                """);
        api.get("/customers/{id}", id).andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"email\": \"ada@example.com\"}",
            "{\"name\": \"   \", \"email\": \"ada@example.com\"}",
            "{\"name\": \"Ada\"}",
            "{\"name\": \"Ada\", \"email\": \"not-an-email\"}"
    })
    void invalidCustomerIsRejected(String body) throws Exception {
        new Api(mockMvc).post("/customers", body).andExpect(status().isBadRequest());
    }

    @Test
    void nameLongerThan100CharactersIsRejected() throws Exception {
        new Api(mockMvc).post("/customers", "{\"name\": \"" + "x".repeat(101) + "\", \"email\": \"a@example.com\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void nameOf100CharactersIsAccepted() throws Exception {
        new Api(mockMvc).post("/customers", "{\"name\": \"" + "x".repeat(100) + "\", \"email\": \"a@example.com\"}")
                .andExpect(status().isCreated());
    }
}
