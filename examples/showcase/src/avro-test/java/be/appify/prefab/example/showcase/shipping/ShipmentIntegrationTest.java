package be.appify.prefab.example.showcase.shipping;

import be.appify.prefab.example.showcase.ordering.OrderClient;
import be.appify.prefab.example.showcase.support.Shop;
import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.TestEventConsumer;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Schema-first Avro events: the event record is generated from an {@code .avsc} file. The shipment is called through
 * MockMvc because its generated test client is not written to the test sources (TASK-368).
 */
@IntegrationTest
class ShipmentIntegrationTest {

    private static final JsonMapper JSON = new JsonMapper();

    @Autowired
    MockMvc mockMvc;
    @Autowired
    OrderClient orders;
    @Autowired
    Shop shop;
    @TestEventConsumer(topic = "showcase.shipment")
    EventConsumer<ShipmentDispatched> dispatched;
    @Value("${spring.kafka.producer.properties.schema.registry.url}")
    String schemaRegistryUrl;

    @Test
    void placeOrder_preparesAShipmentThatWaitsForDispatch() throws Exception {
        var orderId = placeOrder();

        var shipment = shipmentOf(orderId);

        assertThat(shipment.path("carrier").isNull()).isTrue();
        assertThat(shipment.path("trackingNumber").isNull()).isTrue();
    }

    @Test
    void dispatch_publishesTheEventGeneratedFromTheSchemaFile() throws Exception {
        var orderId = placeOrder();
        var shipmentId = shipmentOf(orderId).path("id").asString();

        mockMvc.perform(post("/shipments/{id}/dispatch", shipmentId)
                        .with(user("test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrier\": \"DHL\", \"trackingNumber\": \"JD0001\"}"))
                .andExpect(status().isOk());

        await().atMost(ofSeconds(30)).untilAsserted(() -> assertThat(dispatched.messages()).anySatisfy(event -> {
            assertThat(event.shipmentId()).isEqualTo(shipmentId);
            assertThat(event.orderId()).isEqualTo(orderId);
            assertThat(event.carrier()).isEqualTo("DHL");
            assertThat(event.trackingNumber()).isEqualTo("JD0001");
        }));
        assertThat(registrySubjects()).as("schema registry subjects").containsIgnoringCase("showcase.shipment");
    }

    private String placeOrder() throws Exception {
        return orders.createOrder(shop.registerCustomer(), shop.stockedProduct(10).id(), 1, BigDecimal.TEN);
    }

    private JsonNode shipmentOf(String orderId) {
        return await().atMost(ofSeconds(20)).until(() -> {
            var body = mockMvc.perform(get("/shipments").param("order", orderId).with(user("test")))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            return JSON.readTree(body).path("content").path(0);
        }, found -> !found.isMissingNode());
    }

    private String registrySubjects() throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(schemaRegistryUrl + "/subjects")).build(),
                HttpResponse.BodyHandlers.ofString()).body();
    }
}
