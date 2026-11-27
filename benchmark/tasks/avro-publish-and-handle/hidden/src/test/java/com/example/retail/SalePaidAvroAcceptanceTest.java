package com.example.retail;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class SalePaidAvroAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Value("${spring.kafka.producer.properties.schema.registry.url}")
    String registryUrl;

    @Test
    void paidSalesEndUpInTheCashDrawer() throws Exception {
        var api = new Api(mockMvc);
        var register = api.create("/cash-registers", "{\"name\": \"Till 1\"}");
        var other = api.create("/cash-registers", "{\"name\": \"Till 2\"}");
        var first = api.create("/sales", "{\"cashRegister\": \"%s\", \"amount\": 12.5}".formatted(register));
        var second = api.create("/sales", "{\"cashRegister\": \"%s\", \"amount\": 7.5}".formatted(register));

        api.postEmpty("/sales/{id}/pay", first).andExpect(status().isOk());
        api.postEmpty("/sales/{id}/pay", second).andExpect(status().isOk());

        Api.eventually(Duration.ofSeconds(30), () -> assertThat(
                Api.number(api.get("/cash-registers/{id}", register), "$.cashInDrawer")).isCloseTo(20.0, within(0.001)));
        assertThat(Api.number(api.get("/cash-registers/{id}", other), "$.cashInDrawer")).isCloseTo(0.0, within(0.001));
        assertSchemaRegistered("sales");
        assertSalePaidSchemaNamespace();
    }

    private void assertSchemaRegistered(String fragment) throws Exception {
        var response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(registryUrl + "/subjects")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.body()).as("schema registry subjects (events must be Avro)").containsIgnoringCase(fragment);
    }

    private void assertSalePaidSchemaNamespace() throws Exception {
        var response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(registryUrl + "/subjects/sales-value/versions/latest")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.body()).as("SalePaid Avro schema").contains("\"com.example.retail\"");
    }
}
