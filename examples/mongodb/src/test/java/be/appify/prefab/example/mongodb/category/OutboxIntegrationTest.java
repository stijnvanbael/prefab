package be.appify.prefab.example.mongodb.category;

import be.appify.prefab.core.outbox.OutboxEntry;
import be.appify.prefab.core.outbox.OutboxRelayService;
import be.appify.prefab.core.outbox.OutboxRepository;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.test.IntegrationTest;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the transactional outbox relay with MongoDB.
 *
 * <p>AC#6: Outbox entries saved to the {@code prefab_outbox} collection are relayed to Kafka
 * by the scheduled relay service.</p>
 *
 * <p>AC#7: When the Kafka broker is unavailable, outbox entries remain in the collection
 * until the broker recovers and the relay successfully delivers them.</p>
 */
@IntegrationTest
class OutboxIntegrationTest {

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    OutboxRelayService outboxRelayService;

    @Autowired
    KafkaContainer kafkaContainer;

    @Autowired
    JsonMapper jsonMapper;

    @Test
    void outboxIsEventuallyEmptyAfterRelay() throws Exception {
        outboxRepository.save(pendingEntry());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findPending(100)).isEmpty());
    }

    @Test
    void eventRemainsInOutboxWhenBrokerIsUnavailable() throws Exception {
        var dockerClient = DockerClientFactory.instance().client();
        var containerId = kafkaContainer.getContainerId();

        dockerClient.pauseContainerCmd(containerId).exec();
        try {
            outboxRepository.save(pendingEntry());

            outboxRelayService.relayPendingEvents();

            assertThat(outboxRepository.findPending(100)).isNotEmpty();
        } finally {
            dockerClient.unpauseContainerCmd(containerId).exec();
        }

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findPending(100)).isEmpty());
    }

    private OutboxEntry pendingEntry() {
        var categoryId = Reference.<Category>create();
        var event = new CategoryCreated(categoryId, "Outbox-Test-Category");
        String payload;
        try {
            payload = jsonMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialise test event", e);
        }
        return new OutboxEntry(
                UUID.randomUUID().toString(),
                Category.class.getSimpleName(),
                categoryId.id(),
                CategoryCreated.class.getName(),
                payload,
                Instant.now(),
                null
        );
    }
}
