package be.appify.prefab.test.pubsub;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.gcloud.PubSubEmulatorContainer;

import static be.appify.prefab.test.pubsub.PubSubTestAutoConfiguration.PUBLISH_INITIAL_RPC_TIMEOUT_PROPERTY;
import static be.appify.prefab.test.pubsub.PubSubTestAutoConfiguration.PUBLISH_RPC_TIMEOUT_PROPERTY;
import static be.appify.prefab.test.pubsub.PubSubTestAutoConfiguration.PUBLISH_TIMEOUT_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PubSubTestAutoConfigurationTest {

    private final PubSubEmulatorContainer emulator = mock(PubSubEmulatorContainer.class);

    @Test
    @DisplayName("pubSubPropertiesRegistrar_givenNoConfiguredTimeout_boundsPublishing")
    void pubSubPropertiesRegistrar_givenNoConfiguredTimeout_boundsPublishing() {
        when(emulator.getEmulatorEndpoint()).thenReturn("localhost:8085");

        var properties = register(new MockEnvironment());

        assertThat(properties)
                .containsEntry("spring.cloud.gcp.pubsub.emulator-host", "localhost:8085")
                .containsEntry(PUBLISH_TIMEOUT_PROPERTY, PubSubTestAutoConfiguration.PUBLISH_TIMEOUT_SECONDS)
                .containsEntry(PUBLISH_INITIAL_RPC_TIMEOUT_PROPERTY,
                        PubSubTestAutoConfiguration.PUBLISH_INITIAL_RPC_TIMEOUT_SECONDS)
                .containsEntry(PUBLISH_RPC_TIMEOUT_PROPERTY, PubSubTestAutoConfiguration.PUBLISH_TIMEOUT_SECONDS);
    }

    @Test
    @DisplayName("pubSubPropertiesRegistrar_givenConfiguredTimeout_keepsConfiguredTimeouts")
    void pubSubPropertiesRegistrar_givenConfiguredTimeout_keepsConfiguredTimeouts() {
        var properties = register(new MockEnvironment().withProperty(PUBLISH_INITIAL_RPC_TIMEOUT_PROPERTY, "30"));

        assertThat(properties).doesNotContainKeys(
                PUBLISH_TIMEOUT_PROPERTY, PUBLISH_INITIAL_RPC_TIMEOUT_PROPERTY, PUBLISH_RPC_TIMEOUT_PROPERTY);
    }

    private Map<String, Object> register(MockEnvironment environment) {
        var values = new HashMap<String, Object>();
        new PubSubTestAutoConfiguration()
                .pubSubPropertiesRegistrar(emulator, environment)
                .accept(new DynamicPropertyRegistry() {
                    @Override
                    public void add(String name, Supplier<Object> valueSupplier) {
                        values.put(name, valueSupplier.get());
                    }
                });
        return values;
    }
}
