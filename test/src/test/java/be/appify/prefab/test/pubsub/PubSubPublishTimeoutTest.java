package be.appify.prefab.test.pubsub;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration;
import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubProperties;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static be.appify.prefab.test.pubsub.PubSubTestAutoConfiguration.PUBLISH_INITIAL_RPC_TIMEOUT_PROPERTY;
import static be.appify.prefab.test.pubsub.PubSubTestAutoConfiguration.PUBLISH_RPC_TIMEOUT_PROPERTY;
import static be.appify.prefab.test.pubsub.PubSubTestAutoConfiguration.PUBLISH_TIMEOUT_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Publishing to an emulator that accepts connections but never answers must fail within the configured bound instead
 * of blocking the test forever.
 */
class PubSubPublishTimeoutTest {
    // The Pub/Sub client refuses a total timeout below 10 seconds
    private static final int MINIMUM_TIMEOUT_SECONDS = 10;
    private static final Duration BOUND = Duration.ofSeconds(30);

    @Test
    @DisplayName("publish_givenUnresponsiveEmulator_failsWithinConfiguredTimeout")
    void publish_givenUnresponsiveEmulator_failsWithinConfiguredTimeout() throws IOException {
        try (var unresponsive = new ServerSocket(0)) {
            publishTo(unresponsive.getLocalPort());
        }
    }

    private void publishTo(int port) {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GcpContextAutoConfiguration.class, GcpPubSubAutoConfiguration.class))
                .withUserConfiguration(Emulator.class)
                .withPropertyValues(
                        "spring.cloud.gcp.project-id=timeout-test",
                        "spring.cloud.gcp.pubsub.emulator-host=localhost:" + port,
                        PUBLISH_TIMEOUT_PROPERTY + "=" + MINIMUM_TIMEOUT_SECONDS,
                        PUBLISH_INITIAL_RPC_TIMEOUT_PROPERTY + "=5",
                        PUBLISH_RPC_TIMEOUT_PROPERTY + "=" + MINIMUM_TIMEOUT_SECONDS)
                .run(context -> {
                    var template = context.getBean(PubSubTemplate.class);
                    var message = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("hello")).build();
                    var start = System.nanoTime();

                    assertThatThrownBy(() -> template.publish("projects/timeout-test/topics/missing", message).join())
                            .isInstanceOf(CompletionException.class);

                    assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(BOUND);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class Emulator {
        @Bean
        NoCredentialsProvider credentialsProvider() {
            return NoCredentialsProvider.create();
        }

        @Bean(name = { "subscriberTransportChannelProvider", "publisherTransportChannelProvider" })
        TransportChannelProvider transportChannelProvider(GcpPubSubProperties properties) {
            var channel = ManagedChannelBuilder.forTarget(properties.getEmulatorHost()).usePlaintext().build();
            return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        }
    }
}
