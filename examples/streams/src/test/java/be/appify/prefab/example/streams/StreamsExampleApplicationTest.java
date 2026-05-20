package be.appify.prefab.example.streams;

import be.appify.prefab.core.kafka.GenericKafkaProducer;
import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.TestEventConsumer;
import be.appify.prefab.test.asserts.EventAssertions;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@IntegrationTest
class StreamsExampleApplicationTest {
    @Autowired
    GenericKafkaProducer kafkaProducer;

    @TestEventConsumer(topic = "${topics.streams.words}")
    EventConsumer<WordEvent> wordsConsumer;

    @TestEventConsumer(topic = "${topics.streams.short-words}")
    EventConsumer<ShortWordEvent> shortWordsConsumer;

    @TestEventConsumer(topic = "${topics.streams.long-words}")
    EventConsumer<LongWordEvent> longWordsConsumer;

    @Test
    void branchAndMerge_shouldRouteWordsAndEmitMergedOutput() {
        kafkaProducer.publish(new StreamEvent("s-1", "hello,world,foo"));
        kafkaProducer.publish(new StreamEvent("s-2", "toolong,mini,tiny"));
        kafkaProducer.publish(new StreamEvent("s-3", "   "));

        EventAssertions.assertThat(wordsConsumer)
                .hasReceivedMessages(6)
                .within(30, TimeUnit.SECONDS)
                .where(events -> {
                    events.extracting(WordEvent::word)
                            .contains("HELLO", "WORLD", "FOO", "TOOLONG", "MINI", "TINY");
                    events.allMatch(event -> !event.word().isBlank());
                });

        EventAssertions.assertThat(shortWordsConsumer)
                .hasReceivedMessages(3)
                .within(30, TimeUnit.SECONDS)
                .where(events -> events.extracting(ShortWordEvent::word).contains("FOO", "MINI", "TINY"));

        EventAssertions.assertThat(longWordsConsumer)
                .hasReceivedMessages(3)
                .within(30, TimeUnit.SECONDS)
                .where(events -> events.extracting(LongWordEvent::word).contains("HELLO", "WORLD", "TOOLONG"));
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

        @Bean
        NewTopic shortWordsTopic(@Value("${topics.streams.short-words}") String topicName) {
            return new NewTopic(topicName, 1, (short) 1);
        }

        @Bean
        NewTopic longWordsTopic(@Value("${topics.streams.long-words}") String topicName) {
            return new NewTopic(topicName, 1, (short) 1);
        }
    }
}
