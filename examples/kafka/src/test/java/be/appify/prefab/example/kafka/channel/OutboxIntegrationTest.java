package be.appify.prefab.example.kafka.channel;

import be.appify.prefab.core.outbox.OutboxRepository;
import be.appify.prefab.core.outbox.OutboxRelayService;
import be.appify.prefab.test.IntegrationTest;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.kafka.KafkaContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
@TestPropertySource(properties = {
        "spring.kafka.producer.properties.max.block.ms=1000",
        "spring.kafka.producer.properties.request.timeout.ms=1000",
        "spring.kafka.producer.properties.delivery.timeout.ms=2000"
})
class OutboxIntegrationTest {

    @Autowired
    ChannelClient channels;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    OutboxRelayService outboxRelayService;

    @Autowired
    KafkaContainer kafkaContainer;

    @Test
    void outboxIsEventuallyEmptyAfterRelay() throws Exception {
        channels.createChannel("outbox-relay-channel");

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findPending(100)).isEmpty());
    }

    @Test
    void eventRemainsInOutboxWhenBrokerIsUnavailable() throws Exception {
        var dockerClient = DockerClientFactory.instance().client();
        var containerId = kafkaContainer.getContainerId();

        dockerClient.pauseContainerCmd(containerId).exec();
        try {
            channels.createChannel("broker-down-channel");

            assertThat(outboxRepository.findPending(100)).isNotEmpty();

            outboxRelayService.relayPendingEvents();

            assertThat(outboxRepository.findPending(100)).isNotEmpty();
        } finally {
            dockerClient.unpauseContainerCmd(containerId).exec();
        }

        // AC#7: after the broker recovers the relay eventually drains the outbox
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findPending(100)).isEmpty());
    }
}
