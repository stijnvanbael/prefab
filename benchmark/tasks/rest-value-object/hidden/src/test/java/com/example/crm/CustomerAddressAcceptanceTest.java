package com.example.crm;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CustomerAddressAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void customerKeepsItsAddress() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/customers", """
                {"name": "Ada", "address": {"street": "Main Street 1", "city": "Ghent", "postalCode": "9000", "country": "BE"}}
                """);

        api.get("/customers/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ada"))
                .andExpect(jsonPath("$.address.street").value("Main Street 1"))
                .andExpect(jsonPath("$.address.city").value("Ghent"))
                .andExpect(jsonPath("$.address.postalCode").value("9000"))
                .andExpect(jsonPath("$.address.country").value("BE"));
    }

    @Test
    void addressCanBeReplaced() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/customers", """
                {"name": "Ada", "address": {"street": "Main Street 1", "city": "Ghent", "postalCode": "9000", "country": "BE"}}
                """);

        api.put("/customers/{id}/address", """
                {"address": {"street": "Kerkstraat 5", "city": "Antwerp", "postalCode": "2000", "country": "BE"}}
                """, id).andExpect(status().isOk());

        api.get("/customers/{id}", id)
                .andExpect(jsonPath("$.address.street").value("Kerkstraat 5"))
                .andExpect(jsonPath("$.address.city").value("Antwerp"));
    }
}
