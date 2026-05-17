package be.appify.prefab.example.streams;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.kafka.KafkaStreamBreakoutAdapter;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Demonstrates the streams DSL with {@code filter}, {@code map}, {@code flatMap},
 * predicate/class-based {@code branch}, and factory {@code merge} in one Kafka Streams topology.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Normalise input via {@code filter}, {@code map}, and {@code flatMap}.</li>
 *   <li>Classify words, then branch by subtype via {@code branch(Class)}.</li>
 *   <li>Write each branch to a dedicated output topic.</li>
 *   <li>Use {@code PrefabStreams.merge(...)} to merge subtype branches into a common supertype.</li>
 * </ol>
 */
@Configuration
class StreamTopologyConfiguration {

    @Bean
    StreamDefinition wordBranchAndMergeTopology(PrefabStreams streams) {
        var words = streams.from(StreamEvent.class)
                .filter(event -> !event.payload().isBlank())
                .map(event -> new StreamEvent(event.id(), event.payload().toUpperCase()))
                .breakout(new KafkaStreamBreakoutAdapter<>(
                        nativeStream -> nativeStream.selectKey((key, value) -> value.id())
                ))
                .flatMap(event -> Arrays.stream(event.payload().split(","))
                        .map(word -> new WordEvent(UUID.randomUUID().toString(), word.strip()))
                        .toList());

        var classifiedWords = words.map(word -> word.word().length() <= 4
                ? (ClassifiedWordEvent) new ShortWordEvent(word.id(), word.word())
                : new LongWordEvent(word.id(), word.word()));

        var shortWords = classifiedWords.branch(ShortWordEvent.class);
        var longWords = classifiedWords.branch(LongWordEvent.class);

        shortWords.to(ShortWordEvent.class);
        longWords.to(LongWordEvent.class);

        return streams.merge(shortWords, longWords)
                .map(word -> new WordEvent(word.id(), word.word()))
                .to(WordEvent.class);
    }
}
