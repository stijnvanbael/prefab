package be.appify.prefab.streams.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import be.appify.prefab.core.pubsub.PubSubDeserializer;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import be.appify.prefab.streams.AggregationWindow;
import be.appify.prefab.streams.ContextualStreamProcessor;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.StreamRecord;
import be.appify.prefab.streams.engine.StreamMessage;
import com.google.pubsub.v1.PubsubMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.gcloud.PubSubEmulatorContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = PubSubStreamsIntegrationTest.Application.class, properties = {
        "spring.application.name=streams-integration",
        "spring.cloud.gcp.project-id=streams-integration",
        "spring.main.web-application-type=none"
})
class PubSubStreamsIntegrationTest {
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final PubSubEmulatorContainer PUBSUB =
            new PubSubEmulatorContainer("gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators");

    @Autowired PubSubUtil util;
    @Autowired PubSubStreamSink sink;
    @Autowired PubSubDeserializer deserializer;
    @Autowired GlobalStore<String, Profile> profiles;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        PUBSUB.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.cloud.gcp.pubsub.emulator-host", PUBSUB::getEmulatorEndpoint);
    }

    @AfterAll
    static void stopContainers() {
        PUBSUB.stop();
        POSTGRES.stop();
    }

    @Test
    void processesAggregatesWindowsJoinGlobalStoreAndRedelivery() {
        var counts = subscribe("counts");
        var windows = subscribe("windows");
        var joins = subscribe("joins");
        var answers = subscribe("answers");
        var time = Instant.parse("2026-01-01T00:00:00Z");

        publish("profiles", "profile-1", new Profile("key", "gold"), time);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(profiles.get("key")).contains(new Profile("key", "gold")));
        publish("lookups", "lookup-1", new Lookup("key"), time);
        publish("words", "word-1", new Word("key", "one"), time);
        publish("words", "word-1", new Word("key", "one"), time);
        publish("words", "word-2", new Word("key", "two"), time.plusSeconds(1));
        publish("left", "left-1", new Left("key", "hello"), time);
        publish("right", "right-1", new Right("key", "ready"), time.plusSeconds(1));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(values("counts", counts, Count.class)).containsExactly(new Count("key", 1), new Count("key", 2));
            assertThat(values("windows", windows, WindowCount.class))
                    .containsExactly(new WindowCount("key", 1), new WindowCount("key", 2));
            assertThat(values("joins", joins, Joined.class)).containsExactly(new Joined("key", "hello", "ready"));
            assertThat(values("answers", answers, Answer.class)).containsExactly(new Answer("key", "gold"));
        });
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(counts).hasSize(2);
            assertThat(windows).hasSize(2);
        });
    }

    private List<PubsubMessage> subscribe(String topic) {
        var messages = new CopyOnWriteArrayList<PubsubMessage>();
        util.subscribeWithHandle(SubscriptionRequest.raw(topic, "test-output-" + topic, messages::add));
        return messages;
    }

    private void publish(String topic, String id, Keyed<String> value, Instant time) {
        sink.publish(topic, new StreamMessage(id, value.key(), value, Map.of(), time));
    }

    private <T> List<T> values(String topic, List<PubsubMessage> messages, Class<T> type) {
        return messages.stream()
                .map(message -> deserializer.deserialize(topic, message.getData(), type, message.getAttributesMap()))
                .toList();
    }

    @Event(topic = "words") record Word(String key, String text) implements Keyed<String> {}
    @Event(topic = "counts") record Count(String key, int count) implements Keyed<String> {}
    @Event(topic = "windows") record WindowCount(String key, int count) implements Keyed<String> {}
    @Event(topic = "left") record Left(String key, String text) implements Keyed<String> {}
    @Event(topic = "right") record Right(String key, String status) implements Keyed<String> {}
    @Event(topic = "joins") record Joined(String key, String text, String status) implements Keyed<String> {}
    @Event(topic = "profiles") record Profile(String key, String tier) implements Keyed<String> {}
    @Event(topic = "lookups") record Lookup(String key) implements Keyed<String> {}
    @Event(topic = "answers") record Answer(String key, String tier) implements Keyed<String> {}

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(PrefabCoreConfiguration.class)
    static class Application {
        @Bean
        EventRegistryCustomizer events() {
            return registry -> {
                registry.register("words", Word.class, Event.Serialization.JSON);
                registry.register("counts", Count.class, Event.Serialization.JSON);
                registry.register("windows", WindowCount.class, Event.Serialization.JSON);
                registry.register("left", Left.class, Event.Serialization.JSON);
                registry.register("right", Right.class, Event.Serialization.JSON);
                registry.register("joins", Joined.class, Event.Serialization.JSON);
                registry.register("profiles", Profile.class, Event.Serialization.JSON);
                registry.register("lookups", Lookup.class, Event.Serialization.JSON);
                registry.register("answers", Answer.class, Event.Serialization.JSON);
            };
        }

        @Bean
        GlobalStore<String, Profile> profiles(PrefabStreams streams) {
            return streams.globalStore(Profile.class);
        }

        @Bean
        StreamDefinition counts(PrefabStreams streams) {
            return streams.from(Word.class).groupBy(String.class, Word::key)
                    .aggregate(Count.class, () -> new Count("", 0),
                            (word, count) -> new Count(word.key(), count.count() + 1)).to(Count.class);
        }

        @Bean
        StreamDefinition windows(PrefabStreams streams) {
            return streams.from(Word.class).groupBy(String.class, Word::key)
                    .windowedAggregate(AggregationWindow.tumbling(Duration.ofSeconds(10), Duration.ZERO),
                            WindowCount.class, () -> new WindowCount("", 0),
                            (word, count) -> new WindowCount(word.key(), count.count() + 1)).to(WindowCount.class);
        }

        @Bean
        StreamDefinition joins(PrefabStreams streams) {
            return streams.from(Left.class)
                    .join(streams.from(Right.class), JoinWindow.of(Duration.ofSeconds(5), Duration.ZERO),
                            (left, right) -> new Joined(left.key(), left.text(), right.status())).to(Joined.class);
        }

        @Bean
        StreamDefinition lookups(PrefabStreams streams, GlobalStore<String, Profile> profiles) {
            return streams.from(Lookup.class).process(new LookupProcessor(profiles)).to(Answer.class);
        }
    }

    private static final class LookupProcessor extends ContextualStreamProcessor<String, Lookup, String, Answer> {
        private final GlobalStore<String, Profile> profiles;

        private LookupProcessor(GlobalStore<String, Profile> profiles) {
            this.profiles = profiles;
        }

        @Override public Collection<GlobalStore<?, ?>> globalStores() { return List.of(profiles); }

        @Override
        public void process(StreamRecord<String, Lookup> record) {
            forward(record.key(), new Answer(record.key(), profiles.get(record.key()).orElseThrow().tier()));
        }
    }
}
