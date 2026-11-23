package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.KafkaSchemaRegistrySupport;
import be.appify.prefab.core.kafka.PrefabRegistryConfiguration;
import be.appify.prefab.core.versioning.EventMigrator;
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
}
