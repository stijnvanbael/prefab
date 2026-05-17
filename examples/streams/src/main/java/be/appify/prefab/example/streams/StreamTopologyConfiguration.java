package be.appify.prefab.example.streams;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Demonstrates the streams DSL with {@code filter}, {@code map}, {@code flatMap}, {@code branch},
 * and {@code merge} in one Kafka Streams topology.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Normalise input via {@code filter}, {@code map}, and {@code flatMap}.</li>
 *   <li>{@code branch} into short and long words.</li>
 *   <li>Write each branch to a dedicated output topic.</li>
 *   <li>{@code merge} both branches and write all words to a combined output topic.</li>
 * </ol>
 */
@Configuration
class StreamTopologyConfiguration {

    @Bean
    StreamDefinition wordBranchAndMergeTopology(PrefabStreams streams) {
        var words = streams.from(StreamEvent.class)
                .filter(event -> !event.payload().isBlank())
                .map(event -> new StreamEvent(event.id(), event.payload().toUpperCase()))
                .flatMap(event -> Arrays.stream(event.payload().split(","))
                        .map(word -> new WordEvent(UUID.randomUUID().toString(), word.strip()))
                        .toList());

        var branches = words.branch(
                word -> word.word().length() <= 4,
                word -> true
        );

        branches.get(0)
                .map(word -> new ShortWordEvent(word.id(), word.word()))
                .to(ShortWordEvent.class);
        branches.get(1)
                .map(word -> new LongWordEvent(word.id(), word.word()))
                .to(LongWordEvent.class);

        return branches.get(0)
                .merge(branches.get(1))
                .to(WordEvent.class);
    }
}
