package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import java.util.HashMap;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.StreamsBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;

/** Kafka-backed baseline streams DSL wiring. */
@Configuration
@ConditionalOnClass(StreamsBuilder.class)
@EnableKafkaStreams
public class StreamsConfiguration {

    /** Constructs a new StreamsConfiguration. */
    public StreamsConfiguration() {
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    KafkaStreamsConfiguration defaultStreamsConfig(
            KafkaConnectionDetails connectionDetails,
            @Value("${spring.kafka.streams.application-id:${spring.application.name}}") String applicationId
    ) {
        var properties = new HashMap<String, Object>();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getProducer().getBootstrapServers());
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde");
        properties.put(StreamsConfig.STATE_DIR_CONFIG, "target/kafka-streams-state");
        return new KafkaStreamsConfiguration(properties);
    }

    @Bean
    KafkaTopicResolver kafkaTopicResolver(KafkaJsonTypeResolver typeResolver) {
        return new KafkaTopicResolver(typeResolver);
    }

    @Bean
    PrefabStreams prefabStreams(
            StreamsBuilder streamsBuilder,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer
    ) {
        return new KafkaPrefabStreams(streamsBuilder, topicResolver, serializer, deserializer);
    }

    @Bean
    SmartInitializingSingleton streamTopologyBootstrap(ObjectProvider<StreamDefinition> streamDefinitions) {
        return () -> streamDefinitions.orderedStream().forEach(StreamDefinition::buildTopology);
    }
}

