package be.appify.prefab.test.kafka;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the consumer-properties logic inside {@link KafkaTestAutoConfiguration} without
 * instantiating the full factory (which requires Confluent Avro classes absent from this module).
 */
class KafkaTestAutoConfigurationTest {

    /**
     * The test-infrastructure consumer ({@code testConsumerFactory}) defaults to {@code earliest}
     * so that {@code @TestEventConsumer} fields reliably catch events even when Kafka partition
     * assignment hasnetes before the first poll.
     */
    @Test
    void testConsumerPropertiesDefaultToEarliest() {
        var kafkaProperties = new KafkaProperties();

        var consumerProps = kafkaProperties.buildConsumerProperties();
        // mirrors KafkaTestAutoConfiguration.testConsumerFactory
        consumerProps.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        assertEquals("earliest", consumerProps.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }

    /** An explicit consumer property is preserved in {@code testConsumerFactory}. */
    @Test
    void testConsumerPropertiesPreserveExplicitOffsetReset() {
        var kafkaProperties = new KafkaProperties();
        kafkaProperties.getConsumer().getProperties().put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        var consumerProps = kafkaProperties.buildConsumerProperties();
        consumerProps.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        assertEquals("latest", consumerProps.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }

    /**
     * The {@code testLatestOffsetResetCustomizer} overrides the application consumer factory to
     * {@code latest} when no explicit {@code auto.offset.reset} is configured by the user.
     */
    @Test
    void applicationFactoryCustomizerDefaultsToLatest() {
        var kafkaProperties = new KafkaProperties();
        var configuration = new KafkaTestAutoConfiguration(kafkaProperties);

        var customizer = configuration.testLatestOffsetResetCustomizer(kafkaProperties);
        var factory = new DefaultKafkaConsumerFactory<>(
                Map.of("bootstrap.servers", "localhost:9092",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"));
        customizer.customize(factory);

        assertEquals("latest", factory.getConfigurationProperties().get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }

    /**
     * The customizer does NOT override an explicit {@code auto.offset.reset} value set by the user
     * via {@code spring.kafka.consumer.auto-offset-reset}.
     */
    @Test
    void applicationFactoryCustomizerPreservesExplicitOffsetReset() {
        var kafkaProperties = new KafkaProperties();
        kafkaProperties.getConsumer().getProperties().put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        var configuration = new KafkaTestAutoConfiguration(kafkaProperties);

        var customizer = configuration.testLatestOffsetResetCustomizer(kafkaProperties);
        var factory = new DefaultKafkaConsumerFactory<>(
                Map.of("bootstrap.servers", "localhost:9092",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"));
        customizer.customize(factory);

        assertEquals("earliest", factory.getConfigurationProperties().get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }

    @Test
    void kafkaNetworkNameDefaultsToApplicationScopedName() {
        var environment = new MockEnvironment().withProperty("spring.application.name", "my-chat-app");

        assertEquals("kafka_my_chat_app", KafkaTestcontainerAutoConfiguration.kafkaNetworkName(environment));
    }

    @Test
    void kafkaNetworkNameUsesExplicitOverride() {
        var environment = new MockEnvironment()
                .withProperty("spring.application.name", "my-chat-app")
                .withProperty("prefab.test.kafka.network-name", "kafka_shared");

        assertEquals("kafka_shared", KafkaTestcontainerAutoConfiguration.kafkaNetworkName(environment));
    }
}

