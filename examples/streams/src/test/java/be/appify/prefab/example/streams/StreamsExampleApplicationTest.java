package be.appify.prefab.example.streams;

import be.appify.prefab.example.streams.infrastructure.kafka.StreamEventKafkaProducer;
import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.TestEventConsumer;
import be.appify.prefab.test.asserts.EventAssertions;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@IntegrationTest
class StreamsExampleApplicationTest {
    @Autowired
    StreamEventKafkaProducer streamEventPublisher;

    @TestEventConsumer(topic = "${topics.streams.words}")
    EventConsumer<WordEvent> wordsConsumer;

    @Test
    void filterMapFlatMap_shouldExtractUpperCasedWordsFromCommaSeparatedPayload() {
        streamEventPublisher.publish(new StreamEvent("s-1", "hello,world,foo"));
        streamEventPublisher.publish(new StreamEvent("s-2", "   "));

        EventAssertions.assertThat(wordsConsumer)
                .hasReceivedMessages(3)
                .within(30, TimeUnit.SECONDS)
                .where(events -> {
                    events.extracting(WordEvent::word)
                            .contains("HELLO", "WORLD", "FOO");
                    events.allMatch(event -> !event.word().isBlank());
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TopicConfiguration {
        @Bean
        NewTopic streamInputTopic(@Value("${topics.streams.input}") String topicName) {
            return new NewTopic(topicName, 1, (short) 1);
        }

        @Bean
        NewTopic streamWordsTopic(@Value("${topics.streams.words}") String topicName) {
            return new NewTopic(topicName, 1, (short) 1);
        }
    }
}
