package com.example.ordering;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class OrderTotalsAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void responseContainsTotalAndLineCount() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/orders", """
                {"customer": "Ada", "lines": [
                    {"product": "pen", "quantity": 3, "unitPrice": 1.50},
                    {"product": "notebook", "quantity": 2, "unitPrice": 4.25}
                ]}
                """);

        var order = api.get("/orders/{id}", id).andExpect(status().isOk()).andExpect(jsonPath("$.lineCount").value(2));
        assertThat(Api.number(order, "$.total")).isCloseTo(13.0, within(0.001));
    }

    @Test
    void emptyOrderHasZeroTotal() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/orders", """
                {"customer": "Ada", "lines": []}
                """);

        var order = api.get("/orders/{id}", id).andExpect(jsonPath("$.lineCount").value(0));
        assertThat(Api.number(order, "$.total")).isCloseTo(0.0, within(0.001));
    }

    @Test
    void computedValuesSentByClientsAreIgnored() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/orders", """
                {"customer": "Ada", "total": 999, "lines": [{"product": "pen", "quantity": 1, "unitPrice": 2}]}
                """);

        assertThat(Api.number(api.get("/orders/{id}", id), "$.total")).isCloseTo(2.0, within(0.001));
    }
}
