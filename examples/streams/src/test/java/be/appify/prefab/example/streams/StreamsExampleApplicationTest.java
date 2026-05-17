package be.appify.prefab.example.streams;

import be.appify.prefab.test.IntegrationTest;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class StreamsExampleApplicationTest {
    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    KafkaConnectionDetails kafkaConnectionDetails;

    @Value("${topics.streams.input}")
    String inputTopic;
    @Value("${topics.streams.words}")
    String wordsTopic;

    @Test
    void filterMapFlatMap_shouldExtractUpperCasedWordsFromCommaSeparatedPayload() {
        kafkaTemplate.send(inputTopic, "s-1", new StreamEvent("s-1", "hello,world,foo")).join();
        // blank payload should be filtered out and produce no words
        kafkaTemplate.send(inputTopic, "s-2", new StreamEvent("s-2", "   ")).join();

        try (var consumer = new KafkaConsumer<String, byte[]>(consumerProperties())) {
            consumer.subscribe(java.util.List.of(wordsTopic));
            Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var records = consumer.poll(Duration.ofMillis(500));
                assertThat(records.records(wordsTopic)).hasSizeGreaterThanOrEqualTo(3);
                var payloads = records.records(wordsTopic).stream()
                        .map(r -> new String(r.value()))
                        .toList();
                assertThat(payloads).anyMatch(p -> p.contains("HELLO"));
                assertThat(payloads).anyMatch(p -> p.contains("WORLD"));
                assertThat(payloads).anyMatch(p -> p.contains("FOO"));
            });
        }
    }

    private Properties consumerProperties() {
        var properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionDetails.getConsumer().getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "streams-example-test-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, true);
        return properties;
    }
}
