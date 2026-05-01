package be.appify.prefab.example.mongodb.category;

import be.appify.prefab.core.outbox.OutboxRelayService;
import be.appify.prefab.core.outbox.OutboxRepository;
import be.appify.prefab.test.IntegrationTest;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the transactional outbox pattern with MongoDB.
 *
 * <p>AC#6: Events are persisted to the {@code prefab_outbox} collection in the same
 * MongoDB write as the aggregate and are relayed to Kafka by the scheduled relay service.</p>
 *
 * <p>AC#7: When the Kafka broker is unavailable, outbox entries remain in the collection
 * until the broker recovers and the relay successfully delivers them.</p>
 */
@IntegrationTest
class OutboxIntegrationTest {

    @Autowired
    CategoryClient categories;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    OutboxRelayService outboxRelayService;

    @Autowired(required = false)
    MongoDBContainer mongoDBContainer;

    @Test
    void outboxIsEventuallyEmptyAfterRelay() {
        categories.createCategory("Outbox-Relay-Category");

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findPending(100)).isEmpty());
    }

    @Test
    void eventRemainsInOutboxWhenBrokerIsUnavailable() {
        var dockerClient = DockerClientFactory.instance().client();
        var kafkaContainerId = findKafkaContainerId();
        if (kafkaContainerId == null) {
            return;
        }

        dockerClient.pauseContainerCmd(kafkaContainerId).exec();
        try {
            categories.createCategory("Broker-Down-Category");

            assertThat(outboxRepository.findPending(100)).isNotEmpty();

            outboxRelayService.relayPendingEvents();

            assertThat(outboxRepository.findPending(100)).isNotEmpty();
        } finally {
            dockerClient.unpauseContainerCmd(kafkaContainerId).exec();
        }

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(outboxRepository.findPending(100)).isEmpty());
    }

    private String findKafkaContainerId() {
        return DockerClientFactory.instance().client()
                .listContainersCmd()
                .exec()
                .stream()
                .filter(c -> c.getImage() != null && c.getImage().contains("kafka"))
                .findFirst()
                .map(com.github.dockerjava.api.model.Container::getId)
                .orElse(null);
    }
}
