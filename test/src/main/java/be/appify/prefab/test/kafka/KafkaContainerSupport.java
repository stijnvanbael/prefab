package be.appify.prefab.test.kafka;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Support interface for Kafka container in tests.
 * Implement this interface in your test classes to have a Kafka container available.
 */
public interface KafkaContainerSupport {

    /** The Kafka container. */
    @ServiceConnection
    KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.9.1"))
            .withReuse(true)
            .withExposedPorts(9092, 9093);

    /** Starts the Kafka container before all tests. */
    @BeforeAll
    static void beforeAll() {
        if (!kafkaContainer.isRunning()) {
            kafkaContainer.start();
        }
    }
}

