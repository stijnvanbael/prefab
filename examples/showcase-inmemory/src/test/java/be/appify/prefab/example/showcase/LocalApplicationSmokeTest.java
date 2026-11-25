package be.appify.prefab.example.showcase;

import be.appify.prefab.example.showcase.catalog.application.CategoryRepository;
import be.appify.prefab.example.showcase.customer.application.CustomerOrderHistoryRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.security.user.password=smoke-test"
)
@ActiveProfiles({"local", "local-demo"})
class LocalApplicationSmokeTest {

    private static final String BASIC_AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("user:smoke-test".getBytes(StandardCharsets.UTF_8));
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @LocalServerPort
    int port;

    @Autowired
    CategoryRepository categories;

    @Autowired
    CustomerOrderHistoryRepository orderHistories;

    @Autowired
    @Qualifier("seedDemoCategory")
    ApplicationRunner demoSeed;

    @BeforeEach
    void seedAfterTestCleanup() throws Exception {
        demoSeed.run(new DefaultApplicationArguments(new String[0]));
    }

    @Test
    void startsWithSeedDataServesDocsAndDeliversSerializedEvents() throws Exception {
        assertThat(categories.count()).isEqualTo(1);

        var createCustomer = HttpRequest.newBuilder(uri("/customers"))
                .header("Authorization", BASIC_AUTH)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"firstName":"Ada","lastName":"Lovelace","email":"ada.local@example.com"}
                        """))
                .build();
        var response = HTTP_CLIENT.send(createCustomer, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(orderHistories.count()).isEqualTo(1);
        assertThat(get("/v3/api-docs").statusCode()).isEqualTo(200);
        assertThat(get("/swagger-ui/index.html").statusCode()).isEqualTo(200);
        assertThat(get("/async-api/asyncapi.json").statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", BASIC_AUTH)
                .GET()
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:%d%s".formatted(port, path));
    }
}
