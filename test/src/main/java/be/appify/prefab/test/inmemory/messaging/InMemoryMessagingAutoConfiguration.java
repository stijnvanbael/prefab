package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.KafkaSchemaRegistrySupport;
import be.appify.prefab.core.kafka.PrefabRegistryConfiguration;
import be.appify.prefab.core.pubsub.PubSubDeserializer;
import be.appify.prefab.core.pubsub.PubSubMessageDecoder;
import be.appify.prefab.core.pubsub.PubSubMessageEncoder;
import be.appify.prefab.core.pubsub.PubSubSerializer;
import be.appify.prefab.core.pubsub.PubSubSubscriptions;
import be.appify.prefab.core.sns.SnsMessageEncoder;
import be.appify.prefab.core.sns.SnsSerializer;
import be.appify.prefab.core.sns.SqsDeserializer;
import be.appify.prefab.core.sns.SqsMessageDecoder;
import be.appify.prefab.core.sns.SqsSubscriptions;
import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.core.versioning.EventMigrator;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import io.awspring.cloud.sns.core.SnsTemplate;
import be.appify.prefab.test.inmemory.InMemoryTestAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registers the in-memory event bus when {@code prefab.test.containers} is {@code false}.
 * <p>
 * The bus becomes the dispatcher of every registered event type and delivers events to the generated
 * {@code @KafkaListener} consumers, replacing the Kafka transport that is switched off in this mode.
 */
@AutoConfiguration(after = PrefabRegistryConfiguration.class)
@Conditional(InMemoryTestAutoConfiguration.InMemoryModeCondition.class)
@ConditionalOnBean(EventRegistry.class)
public class InMemoryMessagingAutoConfiguration {

    /** Constructs a new InMemoryMessagingAutoConfiguration. */
    public InMemoryMessagingAutoConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean
    InMemoryEventBus inMemoryEventBus(
            EventRegistry eventRegistry,
            ObjectProvider<MessageCodec> codecs,
            ObjectProvider<PlatformTransactionManager> transactionManager
    ) {
        var outsideTransaction = transactionManager.getIfAvailable() instanceof PlatformTransactionManager manager
                ? notSupported(manager) : TransactionOperations.withoutTransaction();
        return new InMemoryEventBus(eventRegistry, codecs.orderedStream().toList(), outsideTransaction);
    }

    private static TransactionOperations notSupported(PlatformTransactionManager manager) {
        var template = new TransactionTemplate(manager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        return template;
    }

    /** Kafka support: the production serializers and the subscription of {@code @KafkaListener} methods. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({KafkaListener.class, DynamicSerializer.class})
    @EnableConfigurationProperties(KafkaProperties.class)
    static class KafkaMessaging {

        @Bean
        @ConditionalOnMissingBean(DynamicSerializer.class)
        DynamicSerializer dynamicSerializer(
                KafkaProperties kafkaProperties,
                ConversionService conversionService,
                EventRegistry eventRegistry
        ) {
            useMockSchemaRegistryWithoutRegistryUrl(kafkaProperties);
            return new DynamicSerializer(kafkaProperties, conversionService, eventRegistry);
        }

        @Bean
        @ConditionalOnMissingBean(DynamicDeserializer.class)
        DynamicDeserializer dynamicDeserializer(
                KafkaProperties kafkaProperties,
                ConversionService conversionService,
                EventRegistry eventRegistry,
                ObjectProvider<EventMigrator> eventMigrator
        ) {
            useMockSchemaRegistryWithoutRegistryUrl(kafkaProperties);
            return new DynamicDeserializer(kafkaProperties, conversionService, eventRegistry,
                    eventMigrator.getIfAvailable(EventMigrator::none));
        }

        @Bean
        KafkaMessageCodec kafkaMessageCodec(
                DynamicSerializer serializer,
                DynamicDeserializer deserializer,
                EventRegistry eventRegistry
        ) {
            return new KafkaMessageCodec(serializer, deserializer, eventRegistry);
        }

        @Bean
        static KafkaListenerSubscriber kafkaListenerSubscriber(ObjectProvider<InMemoryEventBus> bus) {
            return new KafkaListenerSubscriber(bus);
        }

        private static void useMockSchemaRegistryWithoutRegistryUrl(KafkaProperties kafkaProperties) {
            if (!kafkaProperties.getProperties().containsKey(KafkaSchemaRegistrySupport.SCHEMA_REGISTRY_URL)) {
                KafkaSchemaRegistrySupport.enableMockSchemaRegistry(kafkaProperties);
            }
        }
    }

    /** Pub/Sub support: the production serializers and subscriptions of generated Pub/Sub subscribers. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(PubSubAdmin.class)
    static class PubSubMessaging {

        @Bean
        @ConditionalOnMissingBean(PubSubSerializer.class)
        PubSubSerializer pubSubSerializer(
                JsonUtil jsonUtil,
                EventRegistry eventRegistry,
                ConversionService conversionService
        ) {
            return new PubSubSerializer(jsonUtil, eventRegistry, conversionService);
        }

        @Bean
        @ConditionalOnMissingBean(PubSubDeserializer.class)
        PubSubDeserializer pubSubDeserializer(
                JsonUtil jsonUtil,
                EventRegistry eventRegistry,
                ConversionService conversionService,
                ObjectProvider<EventMigrator> eventMigrator,
                JsonMapper jsonMapper
        ) {
            return new PubSubDeserializer(jsonUtil, eventRegistry, conversionService,
                    eventMigrator.getIfAvailable(EventMigrator::none), jsonMapper);
        }

        @Bean
        PubSubMessageCodec pubSubMessageCodec(
                PubSubSerializer serializer,
                PubSubDeserializer deserializer,
                EventRegistry eventRegistry
        ) {
            return new PubSubMessageCodec(new PubSubMessageEncoder(serializer, eventRegistry::keyFor),
                    deserializer, eventRegistry);
        }

        @Bean
        @ConditionalOnMissingBean(PubSubSubscriptions.class)
        InMemoryPubSubSubscriptions inMemoryPubSubSubscriptions(
                ObjectProvider<InMemoryEventBus> bus,
                PubSubDeserializer deserializer,
                EventRegistry eventRegistry
        ) {
            return new InMemoryPubSubSubscriptions(bus, new PubSubMessageDecoder(deserializer, eventRegistry));
        }
    }

    /** SNS/SQS support: the production serializers and subscriptions of generated SQS subscribers. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(SnsTemplate.class)
    static class SnsMessaging {

        @Bean
        @ConditionalOnMissingBean(SnsSerializer.class)
        SnsSerializer snsSerializer(
                JsonUtil jsonUtil,
                EventRegistry eventRegistry,
                ConversionService conversionService
        ) {
            return new SnsSerializer(jsonUtil, eventRegistry, conversionService);
        }

        @Bean
        @ConditionalOnMissingBean(SqsDeserializer.class)
        SqsDeserializer sqsDeserializer(
                JsonUtil jsonUtil,
                EventRegistry eventRegistry,
                ConversionService conversionService,
                ObjectProvider<EventMigrator> eventMigrator,
                JsonMapper jsonMapper
        ) {
            return new SqsDeserializer(jsonUtil, eventRegistry, conversionService,
                    eventMigrator.getIfAvailable(EventMigrator::none), jsonMapper);
        }

        @Bean
        SnsMessageCodec snsMessageCodec(
                SnsSerializer serializer,
                SqsDeserializer deserializer,
                EventRegistry eventRegistry,
                JsonMapper jsonMapper
        ) {
            return new SnsMessageCodec(new SnsMessageEncoder(serializer), deserializer, eventRegistry, jsonMapper);
        }

        @Bean
        @ConditionalOnMissingBean(SqsSubscriptions.class)
        InMemorySqsSubscriptions inMemorySqsSubscriptions(
                ObjectProvider<InMemoryEventBus> bus,
                SqsDeserializer deserializer
        ) {
            return new InMemorySqsSubscriptions(bus, new SqsMessageDecoder(deserializer));
        }
    }
}
