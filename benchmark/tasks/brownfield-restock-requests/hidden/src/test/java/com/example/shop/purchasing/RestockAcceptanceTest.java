package com.example.shop.purchasing;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import com.example.shop.inventory.StockLevelClient;
import com.example.shop.ordering.OrderClient;
import com.example.shop.support.Shop;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class RestockAcceptanceTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    OrderClient orders;
    @Autowired
    StockLevelClient stockLevels;
    @Autowired
    Shop shop;

    private Api admin() {
        return new Api(mockMvc).with(user("admin").roles("ADMIN"));
    }

    private List<String> requestsFor(String product) throws Exception {
        return Api.read(admin().get("/restock-requests?product={product}", product).andExpect(status().isOk()),
                "$.content[*].id");
    }

    private void awaitAvailable(String stockLevel, int available) throws Exception {
        Api.eventually(Duration.ofSeconds(20), () ->
                assertThat(stockLevels.getStockLevelById(stockLevel).available()).isEqualTo(available));
    }

    @Test
    void runningLowOpensOneRestockRequestThatRefillsTheStock() throws Exception {
        var customer = shop.registerCustomer();
        var stocked = shop.stockedProduct(10);
        var plenty = shop.stockedProduct(100);

        orders.createOrder(customer, stocked.id(), 6, BigDecimal.TEN);
        orders.createOrder(customer, plenty.id(), 6, BigDecimal.TEN);
        awaitAvailable(stocked.stockLevelId(), 4);
        awaitAvailable(plenty.stockLevelId(), 94);

        Api.eventually(Duration.ofSeconds(20), () -> assertThat(requestsFor(stocked.id())).hasSize(1));
        var request = requestsFor(stocked.id()).getFirst();
        admin().get("/restock-requests/{id}", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(20))
                .andExpect(jsonPath("$.status").value("OPEN"));
        assertThat(requestsFor(plenty.id())).isEmpty();

        orders.createOrder(customer, stocked.id(), 1, BigDecimal.TEN);
        awaitAvailable(stocked.stockLevelId(), 3);
        Thread.sleep(500);
        assertThat(requestsFor(stocked.id())).hasSize(1);

        admin().postEmpty("/restock-requests/{id}/fulfilment", request).andExpect(status().isOk());
        admin().get("/restock-requests/{id}", request).andExpect(jsonPath("$.status").value("FULFILLED"));
        awaitAvailable(stocked.stockLevelId(), 23);

        admin().postEmpty("/restock-requests/{id}/fulfilment", request).andExpect(status().isOk());
        Thread.sleep(500);
        assertThat(stockLevels.getStockLevelById(stocked.stockLevelId()).available()).isEqualTo(23);
    }

    @Test
    void onlyAdministratorsSeeAndFulfilRestockRequests() throws Exception {
        var customer = shop.registerCustomer();
        var stocked = shop.stockedProduct(5);
        orders.createOrder(customer, stocked.id(), 1, BigDecimal.TEN);
        Api.eventually(Duration.ofSeconds(20), () -> assertThat(requestsFor(stocked.id())).hasSize(1));
        var request = requestsFor(stocked.id()).getFirst();
        var shopper = new Api(mockMvc).with(user("shopper").roles("CUSTOMER"));

        shopper.get("/restock-requests?product={product}", stocked.id()).andExpect(status().isForbidden());
        shopper.get("/restock-requests/{id}", request).andExpect(status().isForbidden());
        shopper.postEmpty("/restock-requests/{id}/fulfilment", request).andExpect(status().isForbidden());
        var created = admin().post("/restock-requests", "{\"product\": \"%s\", \"quantity\": 5}".formatted(stocked.id()))
                .andReturn().getResponse().getStatus();
        assertThat(created).isIn(404, 405);
    }
}
