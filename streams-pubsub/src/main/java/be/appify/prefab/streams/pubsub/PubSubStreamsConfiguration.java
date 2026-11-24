package be.appify.prefab.streams.pubsub;

import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.pubsub.PubSubConfiguration;
import be.appify.prefab.core.pubsub.PubSubDeserializer;
import be.appify.prefab.core.pubsub.PubSubSerializer;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.engine.InProcessPrefabStreams;
import be.appify.prefab.streams.engine.StreamStateRepository;
import be.appify.prefab.streams.tracing.TracingConfig;
import com.google.cloud.spring.pubsub.core.publisher.PublisherCustomizer;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;

/** Configures the in-process Streams engine for Pub/Sub and transactional state. */
@AutoConfiguration(after = PubSubConfiguration.class,
        afterName = {
                "be.appify.prefab.streams.postgres.PostgresStreamsStateConfiguration",
                "be.appify.prefab.streams.mongodb.MongoStreamsStateConfiguration"
        })
@ConditionalOnBean({PubSubUtil.class, StreamStateRepository.class})
@ConditionalOnMissingBean(PrefabStreams.class)
public class PubSubStreamsConfiguration {
    @Bean
    PublisherCustomizer streamsOrderingCustomizer() {
        return (builder, topic) -> builder.setEnableMessageOrdering(true);
    }

    @Bean
    PubSubStreamSource pubSubStreamSource(
            @Value("${spring.application.name}") String application,
            PubSubUtil util,
            PubSubDeserializer deserializer,
            EventRegistry registry
    ) {
        return new PubSubStreamSource(application, util, deserializer, registry);
    }

    @Bean
    PubSubStreamSink pubSubStreamSink(
            PubSubUtil util, PubSubSerializer serializer, EventRegistry registry, PublisherFactory publisherFactory
    ) {
        return new PubSubStreamSink(util, serializer, registry, publisherFactory);
    }

    @Bean
    InProcessPrefabStreams prefabStreams(
            PubSubStreamSource source,
            PubSubStreamSink sink,
            StreamStateRepository repository,
            EventRegistry registry,
            @Value("${prefab.streams.tracing.enabled:false}") boolean tracing,
            @Value("${prefab.streams.tracing.max-correlation-ids:100}") int maxCorrelationIds,
            @Value("${prefab.streams.inbox-retention:7d}") String retention
    ) {
        return new InProcessPrefabStreams(source, sink, repository, new PubSubStreamTopicResolver(registry),
                new TracingConfig(tracing, maxCorrelationIds), DurationStyle.detectAndParse(retention));
    }

    @Bean
    SmartLifecycle pubSubStreamsLifecycle(
            InProcessPrefabStreams streams, ObjectProvider<StreamDefinition> definitions
    ) {
        return new SmartLifecycle() {
            private InProcessPrefabStreams.RunningTopology topology;

            @Override
            public void start() {
                if (topology == null) {
                    definitions.orderedStream().toList();
                    topology = streams.start();
                }
            }

            @Override
            public void stop() {
                if (topology != null) {
                    topology.close();
                    topology = null;
                }
            }

            @Override
            public boolean isRunning() {
                return topology != null;
            }
        };
    }
}
