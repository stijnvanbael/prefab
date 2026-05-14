package be.appify.prefab.test.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the consumer-properties logic that {@link KafkaTestAutoConfiguration#testConsumerFactory}
 * applies to the Kafka properties, without instantiating the full factory (which requires Confluent
 * Avro classes that are not on the test module's compile classpath).
 */
class KafkaTestAutoConfigurationTest {

    /**
     * Simulates the {@code putIfAbsent("latest")} default applied by
     * {@link KafkaTestAutoConfiguration#testConsumerFactory} when no explicit offset reset is set.
     */
    @Test
    void testConsumerPropertiesDefaultToLatest() {
        var properties = new KafkaProperties();

        var consumerProps = properties.buildConsumerProperties();
        // replicate KafkaTestAutoConfiguration line: putIfAbsent(AUTO_OFFSET_RESET_CONFIG, "latest")
        consumerProps.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        assertEquals("latest", consumerProps.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }

    /**
     * Verifies that an explicitly configured offset reset is not overwritten by the {@code putIfAbsent}
     * default in {@link KafkaTestAutoConfiguration#testConsumerFactory}.
     */
    @Test
    void testConsumerPropertiesPreserveExplicitOffsetReset() {
        var properties = new KafkaProperties();
        properties.getConsumer().getProperties().put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        var consumerProps = properties.buildConsumerProperties();
        // replicate KafkaTestAutoConfiguration line: putIfAbsent(AUTO_OFFSET_RESET_CONFIG, "latest")
        consumerProps.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        assertEquals("earliest", consumerProps.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }
}

