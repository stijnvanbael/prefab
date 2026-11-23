package be.appify.prefab.streams.snssqs;

import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.sns.SnsConfiguration;
import be.appify.prefab.core.sns.SnsSerializer;
import be.appify.prefab.core.sns.SqsUtil;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.engine.InProcessPrefabStreams;
import be.appify.prefab.streams.engine.StreamSink;
import be.appify.prefab.streams.engine.StreamSource;
import be.appify.prefab.streams.engine.StreamStateRepository;
import be.appify.prefab.streams.tracing.TracingConfig;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Bean;

/**
 * Runs the Prefab Streams DSL on SNS/SQS with the in-process engine, when the application has SNS/SQS support and a
 * {@link StreamStateRepository}, for example the Postgres one of {@code prefab-streams-postgres}.
 *
 * <p>The topology starts once all {@link StreamDefinition} beans exist, and stops with the application context.
 */
@AutoConfiguration(
        after = SnsConfiguration.class,
        afterName = "be.appify.prefab.streams.postgres.PostgresStreamsStateConfiguration")
@ConditionalOnClass(SnsTemplate.class)
@ConditionalOnBean({SqsUtil.class, StreamStateRepository.class})
@EnableConfigurationProperties(SnsSqsStreamsProperties.class)
public class SnsSqsStreamsConfiguration {

    /** Constructs a new SnsSqsStreamsConfiguration. */
    public SnsSqsStreamsConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean
    SnsSqsTopicResolver snsSqsTopicResolver(EventRegistry eventRegistry) {
        return new SnsSqsTopicResolver(eventRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(StreamSource.class)
    SnsSqsStreamSource snsSqsStreamSource(
            SqsUtil sqsUtil,
            EventRegistry eventRegistry,
            @Value("${spring.application.name}") String application,
            SnsSqsStreamsProperties properties
    ) {
        return new SnsSqsStreamSource(sqsUtil, eventRegistry, application, properties.visibilityTimeout());
    }

    @Bean
    @ConditionalOnMissingBean(StreamSink.class)
    SnsSqsStreamSink snsSqsStreamSink(
            SnsTemplate snsTemplate,
            SqsUtil sqsUtil,
            SnsSerializer serializer,
            EventRegistry eventRegistry
    ) {
        return new SnsSqsStreamSink(snsTemplate, sqsUtil, serializer, eventRegistry);
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
            SnsSqsTopicResolver topicResolver,
            TracingConfig tracing,
            @Value("${prefab.streams.inbox-retention:7d}") String inboxRetention
    ) {
        return new InProcessPrefabStreams(
                source, sink, repository, topicResolver, tracing, DurationStyle.detectAndParse(inboxRetention));
    }

    /**
     * Starts the topology after every singleton exists, so all stream definitions are declared. Streams that are
     * declared in {@code StreamDefinition} beans are picked up through the {@link PrefabStreams} they are built on.
     */
    @Bean
    SnsSqsTopologyLifecycle snsSqsTopologyLifecycle(
            InProcessPrefabStreams streams,
            ObjectProvider<StreamDefinition> streamDefinitions
    ) {
        return new SnsSqsTopologyLifecycle(streams, streamDefinitions);
    }
}
