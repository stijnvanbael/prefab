package be.appify.prefab.asyncapi;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsyncApiControllerTest {

    @Test
    @DisplayName("asyncApiJson() resolves Spring property placeholders in channel names")
    void asyncApiJson_resolvesSpringPropertyPlaceholders() throws IOException {
        var environment = new MockEnvironment();
        environment.setProperty("topics.orders", "real-orders-topic");
        var controller = new AsyncApiController(environment);

        var response = controller.asyncApiJson();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody())
                .contains("\"real-orders-topic\"")
                .doesNotContain("${topics.orders}");
    }

    @Test
    @DisplayName("asyncApiJson() throws when a placeholder cannot be resolved")
    void asyncApiJson_throwsWhenPlaceholderNotResolved() {
        var environment = new MockEnvironment(); // no property defined
        var controller = new AsyncApiController(environment);

        assertThatThrownBy(controller::asyncApiJson)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topics.orders");
    }
}

