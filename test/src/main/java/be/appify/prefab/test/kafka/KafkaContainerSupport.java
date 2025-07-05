package be.appify.prefab.test.kafka;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public interface KafkaContainerSupport {

    @ServiceConnection
    KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.9.1"))
            .withReuse(true)
            .withExposedPorts(9092, 9093);

    @BeforeAll
    static void beforeAll() {
        if (!kafkaContainer.isRunning()) {
            kafkaContainer.start();
        }
    }
}

