package be.appify.prefab.test.inmemory.streams;

import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.engine.InMemoryStreamStateRepository;
import be.appify.prefab.streams.engine.InProcessPrefabStreams;
import be.appify.prefab.streams.engine.StreamSink;
import be.appify.prefab.streams.engine.StreamSource;
import be.appify.prefab.streams.engine.StreamStateRepository;
import be.appify.prefab.streams.tracing.TracingConfig;
import be.appify.prefab.test.inmemory.InMemoryTestAutoConfiguration;
import be.appify.prefab.test.inmemory.messaging.InMemoryEventBus;
import be.appify.prefab.test.inmemory.messaging.InMemoryMessagingAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * Runs the Prefab Streams DSL on the in-memory event bus when {@code prefab.test.containers} is {@code false} and the
 * application has the in-process streams engine ({@code prefab-streams-engine}).
 *
 * <p>The sources of a topology subscribe to the bus, its sinks publish on the bus, and its state lives in an
 * in-memory {@link StreamStateRepository} that is cleared before every test. The topology starts once all
 * {@link StreamDefinition} beans exist, and stops with the application context.
 */
@AutoConfiguration(after = InMemoryMessagingAutoConfiguration.class)
@Conditional(InMemoryTestAutoConfiguration.InMemoryModeCondition.class)
@ConditionalOnClass(InProcessPrefabStreams.class)
@ConditionalOnBean(InMemoryEventBus.class)
public class InMemoryStreamsAutoConfiguration {

    /** Constructs a new InMemoryStreamsAutoConfiguration. */
    public InMemoryStreamsAutoConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean(StreamStateRepository.class)
    InMemoryStreamStateRepository inMemoryStreamStateRepository() {
        return new InMemoryStreamStateRepository();
    }

    @Bean
    @ConditionalOnBean(InMemoryStreamStateRepository.class)
    StreamStateReset inMemoryStreamStateReset(InMemoryStreamStateRepository repository) {
        return repository::clear;
    }

    @Bean
    @ConditionalOnMissingBean(StreamSource.class)
    InMemoryBusStreamSource inMemoryBusStreamSource(InMemoryEventBus bus) {
        return new InMemoryBusStreamSource(bus);
    }

    @Bean
    @ConditionalOnMissingBean(StreamSink.class)
    InMemoryBusStreamSink inMemoryBusStreamSink(InMemoryEventBus bus, EventRegistry eventRegistry) {
        return new InMemoryBusStreamSink(bus, eventRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(TracingConfig.class)
    TracingConfig streamsTracingConfig(
            @Value("${prefab.streams.tracing.enabled:false}") boolean enabled,
            @Value("${prefab.streams.tracing.max-correlation-ids:100}") int maxCorrelationIds
    ) {
        return new TracingConfig(enabled, maxCorrelationIds);
    }

    @Bean
    @ConditionalOnMissingBean(PrefabStreams.class)
    InProcessPrefabStreams prefabStreams(
            StreamSource source,
            StreamSink sink,
            StreamStateRepository repository,
            EventRegistry eventRegistry,
            TracingConfig tracing,
            @Value("${prefab.streams.inbox-retention:7d}") String inboxRetention
    ) {
        return new InProcessPrefabStreams(source, sink, repository, new EventRegistryStreamTopicResolver(eventRegistry),
                tracing, DurationStyle.detectAndParse(inboxRetention));
    }

    @Bean
    @ConditionalOnBean(InProcessPrefabStreams.class)
    InMemoryStreamsLifecycle inMemoryStreamsLifecycle(
            InProcessPrefabStreams streams,
            ObjectProvider<StreamDefinition> streamDefinitions
    ) {
        return new InMemoryStreamsLifecycle(streams, streamDefinitions);
    }
}
