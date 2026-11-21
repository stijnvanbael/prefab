package com.example.billing;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class InvoiceRangeAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private Api seed() throws Exception {
        var api = new Api(mockMvc);
        api.create("/invoices", "{\"number\": \"I-1\", \"amount\": 5, \"issuedOn\": \"2026-01-15\"}");
        api.create("/invoices", "{\"number\": \"I-2\", \"amount\": 10, \"issuedOn\": \"2026-02-15\"}");
        api.create("/invoices", "{\"number\": \"I-3\", \"amount\": 50, \"issuedOn\": \"2026-03-01\"}");
        api.create("/invoices", "{\"number\": \"I-4\", \"amount\": 50.01, \"issuedOn\": \"2026-04-15\"}");
        return api;
    }

    private static List<String> numbers(Api api, String query) throws Exception {
        return Api.read(api.get("/invoices?size=50&" + query).andExpect(status().isOk()), "$.content[*].number");
    }

    @Test
    void filtersByAmountRangeInclusive() throws Exception {
        assertThat(numbers(seed(), "amountGreaterThanOrEqual=10&amountLessThanOrEqual=50"))
                .containsExactlyInAnyOrder("I-2", "I-3");
    }

    @Test
    void boundsCanBeUsedAlone() throws Exception {
        var api = seed();
        assertThat(numbers(api, "amountGreaterThanOrEqual=50")).containsExactlyInAnyOrder("I-3", "I-4");
        assertThat(numbers(api, "amountLessThanOrEqual=10")).containsExactlyInAnyOrder("I-1", "I-2");
    }

    @Test
    void filtersByIssueDateAndAmount() throws Exception {
        assertThat(numbers(seed(), "issuedOnGreaterThanOrEqual=2026-03-01&amountLessThanOrEqual=50"))
                .containsExactly("I-3");
    }
}
