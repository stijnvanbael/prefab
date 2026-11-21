package com.example.billing;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class InvoiceSecurityAcceptanceTest {
    private static final String INVOICE = "{\"number\": \"I-1\", \"amount\": 100}";

    @Autowired
    MockMvc mockMvc;

    private Api as(String... roles) {
        return new Api(mockMvc).with(user("someone").roles(roles));
    }

    @Test
    void onlyAccountantsCreateInvoices() throws Exception {
        as("ACCOUNTANT").create("/invoices", INVOICE);
        as("USER").post("/invoices", INVOICE).andExpect(status().isForbidden());
        new Api(mockMvc).post("/invoices", INVOICE).andExpect(status().isUnauthorized());
    }

    @Test
    void everyAuthenticatedUserReadsInvoices() throws Exception {
        var id = as("ACCOUNTANT").create("/invoices", INVOICE);
        as("USER").get("/invoices/{id}", id).andExpect(status().isOk());
        as("USER").get("/invoices").andExpect(status().isOk());
        new Api(mockMvc).get("/invoices/{id}", id).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminsDeleteInvoices() throws Exception {
        var id = as("ACCOUNTANT").create("/invoices", INVOICE);
        as("ACCOUNTANT").delete("/invoices/{id}", id).andExpect(status().isForbidden());
        as("ADMIN").delete("/invoices/{id}", id).andExpect(status().isNoContent());
        as("USER").get("/invoices/{id}", id).andExpect(status().isNotFound());
    }
}
