package be.appify.prefab.test.pubsub;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.gcloud.PubSubEmulatorContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two Spring contexts that are both held by the test context cache each own an emulator. Starting the emulator of
 * the second context must not remove the emulator of the first one.
 */
class PubSubEmulatorSharingIntegrationTest {
    private static final String PROJECT = "sharing-test";
    private static final String TOPIC = "sharing-topic";

    private final PubSubTestAutoConfiguration configuration = new PubSubTestAutoConfiguration();
    private final MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.application.name", "pubsub-sharing-test");
    private PubSubEmulatorContainer first;
    private PubSubEmulatorContainer second;

    @AfterEach
    void stopEmulators() {
        if (second != null) {
            second.stop();
        }
        if (first != null) {
            first.stop();
        }
    }

    @Test
    @DisplayName("pubSubEmulatorContainer_givenTwoContexts_keepsBothEmulatorsUsable")
    void pubSubEmulatorContainer_givenTwoContexts_keepsBothEmulatorsUsable() throws Exception {
        first = configuration.pubSubEmulatorContainer(environment);
        second = configuration.pubSubEmulatorContainer(environment);

        assertThat(first.getContainerName()).isNotEqualTo(second.getContainerName());
        assertThat(first.isRunning()).isTrue();
        assertThat(second.isRunning()).isTrue();
        assertThat(publish(first)).isNotBlank();
        assertThat(publish(second)).isNotBlank();
    }

    private static String publish(PubSubEmulatorContainer emulator) throws Exception {
        var channel = ManagedChannelBuilder.forTarget(emulator.getEmulatorEndpoint()).usePlaintext().build();
        try {
            var channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            var topic = TopicName.of(PROJECT, TOPIC);
            createTopic(channelProvider, topic);
            var publisher = Publisher.newBuilder(topic)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .build();
            try {
                return publisher.publish(PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("hello")).build())
                        .get(30, TimeUnit.SECONDS);
            } finally {
                publisher.shutdown();
            }
        } finally {
            shutdown(channel);
        }
    }

    private static void createTopic(FixedTransportChannelProvider channelProvider, TopicName topic) throws Exception {
        var settings = TopicAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(NoCredentialsProvider.create())
                .build();
        try (var admin = TopicAdminClient.create(settings)) {
            admin.createTopic(topic);
        }
    }

    private static void shutdown(ManagedChannel channel) throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
