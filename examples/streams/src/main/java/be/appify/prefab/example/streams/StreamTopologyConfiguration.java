package be.appify.prefab.example.streams;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Demonstrates all three stateless operators — {@code filter}, {@code map}, and {@code flatMap} —
 * wired as a single Kafka Streams pipeline.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>{@code filter} — drop events with a blank payload.</li>
 *   <li>{@code map} — upper-case the payload.</li>
 *   <li>{@code flatMap} — split the payload on commas into individual {@link WordEvent} records.</li>
 *   <li>{@code to} — write every word to the words output topic.</li>
 * </ol>
 */
@Configuration
class StreamTopologyConfiguration {

    @Bean
    StreamDefinition wordExtractionTopology(PrefabStreams streams) {
        return streams.from(StreamEvent.class)
                .filter(event -> !event.payload().isBlank())
                .map(event -> new StreamEvent(event.id(), event.payload().toUpperCase()))
                .flatMap(event -> Arrays.stream(event.payload().split(","))
                        .map(word -> new WordEvent(UUID.randomUUID().toString(), word.strip()))
                        .toList())
                .to(WordEvent.class);
    }
}
