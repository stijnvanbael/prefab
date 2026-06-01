package be.appify.prefab.core.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        classes = ActuatorDefaultsIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.gcp.core.enabled=false",
                "spring.cloud.gcp.pubsub.enabled=false",
                "spring.cloud.aws.sns.enabled=false",
                "spring.cloud.aws.sqs.enabled=false"
        })
class ActuatorDefaultsIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Prefab applications expose actuator health endpoint by default")
    void shouldExposeHealthEndpointByDefault() throws IOException, InterruptedException {
        var response = sendGet("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Prefab applications expose actuator info endpoint by default")
    void shouldExposeInfoEndpointByDefault() throws IOException, InterruptedException {
        var response = sendGet("/actuator");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("/actuator/health")
                .contains("/actuator/info");
    }

    private HttpResponse<String> sendGet(String path) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://localhost:" + port + path))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SpringBootApplication
    static class TestApplication {
    }
}

