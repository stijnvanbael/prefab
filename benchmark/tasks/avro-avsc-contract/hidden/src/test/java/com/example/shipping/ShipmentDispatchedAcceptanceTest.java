package com.example.shipping;

import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.TestEventConsumer;
import benchmark.support.Api;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ShipmentDispatchedAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @TestEventConsumer(topic = "shipments")
    EventConsumer<ShipmentDispatched> dispatched;

    @Value("${spring.kafka.producer.properties.schema.registry.url}")
    String registryUrl;

    @Test
    void dispatchPublishesTheContractEvent() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/shipments", "{\"recipient\": \"Ada\"}");

        api.post("/shipments/{id}/dispatch", "{\"carrier\": \"DHL\", \"trackingNumber\": \"JD0001\"}", id)
                .andExpect(status().isOk());

        Api.eventually(Duration.ofSeconds(30), () -> assertThat(dispatched.messages()).anySatisfy(event -> {
            assertThat(event.shipmentId()).isEqualTo(id);
            assertThat(event.carrier()).isEqualTo("DHL");
            assertThat(event.trackingNumber()).isEqualTo("JD0001");
        }));
        var subjects = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(registryUrl + "/subjects")).build(),
                HttpResponse.BodyHandlers.ofString()).body();
        assertThat(subjects).as("schema registry subjects (events must be Avro)").containsIgnoringCase("shipment");
    }

    @Test
    void eventClassIsGeneratedFromTheSchema() {
        assertThat(Path.of("src/main/java/com/example/shipping/ShipmentDispatched.java")).doesNotExist();
        assertThat(Files.exists(Path.of("target/generated-sources/annotations/com/example/shipping/ShipmentDispatched.java")))
                .as("ShipmentDispatched is generated from the AVSC schema").isTrue();
    }
}
