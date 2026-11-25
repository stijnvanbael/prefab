package com.example.shipping;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ShipmentTrackingAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private Api api() {
        return new Api(mockMvc);
    }

    private String shipment(String orderNumber) throws Exception {
        return api().create("/shipments",
                "{\"orderNumber\": \"%s\", \"address\": \"Main Street 1, Ghent\"}".formatted(orderNumber));
    }

    private String scan(String shipment, String location, String type) throws Exception {
        return api().create("/shipments/{id}/scans",
                "{\"location\": \"%s\", \"type\": \"%s\"}".formatted(location, type), shipment);
    }

    private List<String> notifications(String orderNumber) throws Exception {
        return Api.read(api().get("/delivery-notifications?orderNumber={number}", orderNumber)
                .andExpect(status().isOk()), "$.content[*].id");
    }

    @Test
    void scansMoveTheShipmentToItsDoorAndNotifyOnce() throws Exception {
        var orderNumber = "SO-" + System.nanoTime();
        var shipment = shipment(orderNumber);
        api().get("/shipments/{id}", shipment).andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.scanCount").value(0))
                .andExpect(jsonPath("$.lastLocation").isEmpty());

        scan(shipment, "Antwerp hub", "PICKED_UP");
        Api.eventually(Duration.ofSeconds(20), () -> api().get("/shipments/{id}", shipment)
                .andExpect(jsonPath("$.status").value("PICKED_UP"))
                .andExpect(jsonPath("$.lastLocation").value("Antwerp hub")));
        scan(shipment, "Ghent depot", "IN_TRANSIT");
        Api.eventually(Duration.ofSeconds(20), () -> api().get("/shipments/{id}", shipment)
                .andExpect(jsonPath("$.status").value("IN_TRANSIT")));
        assertThat(notifications(orderNumber)).isEmpty();

        scan(shipment, "Main Street 1", "DELIVERED");
        scan(shipment, "Main Street 1", "DELIVERED");
        scan(shipment, "Ghent depot", "IN_TRANSIT");

        Api.eventually(Duration.ofSeconds(20), () -> api().get("/shipments/{id}", shipment)
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.scanCount").value(5))
                .andExpect(jsonPath("$.lastLocation").value("Ghent depot")));
        Api.eventually(Duration.ofSeconds(20), () -> assertThat(notifications(orderNumber)).hasSize(1));
        api().get("/delivery-notifications?orderNumber={number}", orderNumber)
                .andExpect(jsonPath("$.content[0].shipment").value(shipment))
                .andExpect(jsonPath("$.content[0].address").value("Main Street 1, Ghent"));
        Thread.sleep(500);
        assertThat(notifications(orderNumber)).hasSize(1);
        List<String> types = Api.read(api().get("/shipments/{id}/scans", shipment).andExpect(status().isOk()),
                "$.content[*].type");
        assertThat(types).hasSize(5).contains("PICKED_UP", "IN_TRANSIT", "DELIVERED");
    }

    @Test
    void shipmentsAreListedByStatus() throws Exception {
        var created = shipment("SO-" + System.nanoTime());
        var moving = shipment("SO-" + System.nanoTime());
        scan(moving, "Antwerp hub", "IN_TRANSIT");

        Api.eventually(Duration.ofSeconds(20), () -> {
            List<String> inTransit = Api.read(api().get("/shipments?status=IN_TRANSIT"), "$.content[*].id");
            assertThat(inTransit).contains(moving).doesNotContain(created);
        });
        List<String> fresh = Api.read(api().get("/shipments?status=CREATED"), "$.content[*].id");
        assertThat(fresh).contains(created).doesNotContain(moving);
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        var shipment = shipment("SO-" + System.nanoTime());

        api().post("/shipments", "{\"orderNumber\": \"\", \"address\": \"x\"}").andExpect(status().isBadRequest());
        api().post("/shipments/{id}/scans", "{\"location\": \"Hub\", \"type\": \"LOST\"}", shipment)
                .andExpect(status().isBadRequest());
    }
}
