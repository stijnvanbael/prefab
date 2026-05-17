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
    @Value("${topics.streams.output}")
    String outputTopic;

    @Test
    void forwardsStreamEventFromInputTopicToOutputTopic() {
        kafkaTemplate.send(inputTopic, "s-1", new StreamEvent("s-1", "hello-streams")).join();

        try (var consumer = new KafkaConsumer<String, byte[]>(consumerProperties())) {
            consumer.subscribe(java.util.List.of(outputTopic));
            Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var records = consumer.poll(Duration.ofMillis(250));
                assertThat(records.records(outputTopic)).isNotEmpty();
                var firstRecord = records.records(outputTopic).iterator().next();
                assertThat(new String(firstRecord.value()))
                        .contains("s-1")
                        .contains("hello-streams");
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

