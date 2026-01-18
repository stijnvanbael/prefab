package be.appify.prefab.core.kafka;

import be.appify.prefab.core.util.Classes;
import com.google.common.collect.Streams;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonTypeResolver;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import static be.appify.prefab.core.kafka.KafkaUtil.DEFAULT_NOT_RETRYABLE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Configuration class for setting up Kafka producer and consumer factories,
 * listener container factory, and error handling with dead-letter publishing.
 */
@Configuration
@ConditionalOnClass(KafkaListenerContainerFactory.class)
@ComponentScan(basePackageClasses = KafkaJsonTypeResolver.class)
public class KafkaConfiguration {

    /** Constructs a new KafkaConfiguration. */
    public KafkaConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean(ProducerFactory.class)
    ProducerFactory<Object, Object> kafkaProducerFactory(
            KafkaProperties kafkaProperties,
            KafkaConnectionDetails connectionDetails,
            ObjectProvider<DefaultKafkaProducerFactoryCustomizer> customizers,
            @Value("${spring.application.name}") String applicationName,
            @Value("${kafka.transactions.enabled:true}") boolean transactionsEnabled
    ) {
        var properties = kafkaProperties.buildProducerProperties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getProducer().getBootstrapServers());

        var serializer = new DynamicSerializer(kafkaProperties);

        var factory = new DefaultKafkaProducerFactory<>(properties, serializer, serializer);
        if (transactionsEnabled) {
            factory.setTransactionIdPrefix(kafkaProperties.getProducer().getTransactionIdPrefix() != null
                    ? kafkaProperties.getProducer().getTransactionIdPrefix()
                    : "%s-%s".formatted(applicationName, UUID.randomUUID().toString()));
        }
        factory.setMaxAge(Duration.ofDays(21));
        customizers.orderedStream().forEach(customizer -> customizer.customize(factory));
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(KafkaListenerContainerFactory.class)
    ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            ObjectProvider<KafkaTransactionManager<?, ?>> kafkaTransactionManager,
            ObjectProvider<CommonErrorHandler> dltErrorHandler,
            ObjectProvider<ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>>> kafkaContainerCustomizer
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<>();
        kafkaTransactionManager.ifAvailable(tm ->
                factory.getContainerProperties().setKafkaAwareTransactionManager(tm));

        configurer.configure(factory, kafkaConsumerFactory);
        kafkaContainerCustomizer.ifAvailable(factory::setContainerCustomizer);
        dltErrorHandler.ifAvailable(factory::setCommonErrorHandler);
        return factory;
    }

    @Bean
    ConsumerFactory<?, ?> kafkaConsumerFactory(
            KafkaProperties kafkaProperties,
            KafkaConnectionDetails connectionDetails,
            ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers,
            ObjectProvider<KafkaTransactionManager<?, ?>> kafkaTransactionManager,
            JacksonJsonTypeResolver jsonTypeResolver,
            @Value("${spring.application.name}") String applicationName
    ) {
        var properties = kafkaProperties.buildConsumerProperties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getConsumer().getBootstrapServers());
        properties.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, applicationName);
        kafkaTransactionManager.ifAvailable(ignored ->
                properties.putIfAbsent(ConsumerConfig.ISOLATION_LEVEL_CONFIG,
                        KafkaProperties.IsolationLevel.READ_COMMITTED.name()));
        var jsonDeserializer = new JacksonJsonDeserializer<>();
        jsonDeserializer.setTypeResolver(jsonTypeResolver);
        var deserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);
        deserializer.configure(properties, false);
        var factory = new DefaultKafkaConsumerFactory<>(properties, new StringDeserializer(), deserializer);
        customizers.orderedStream().forEach(customizer -> customizer.customize(factory));
        return factory;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "defaultKafkaErrorHandler")
    @SuppressWarnings("unchecked")
    CommonErrorHandler defaultKafkaErrorHandler(
            @Value("${prefab.dlt.retries.limit:5}") Integer maxRetries,
            @Value("${prefab.dlt.retries.initial-interval-ms:1000}") Long initialRetryInterval,
            @Value("${prefab.dlt.retries.multiplier:1.5}") Float backoffMultiplier,
            @Value("${prefab.dlt.retries.max-interval-ms:30000}") Long maxRetryInterval,
            @Value("#{'${prefab.dlt.non-retryable-exceptions:}'.split(',')}") List<String> nonRetryableExceptions,
            DeadLetterPublishingRecoverer deadLetterPublishingRecoverer
    ) {
        var backoff = new ExponentialBackOffWithMaxRetries(maxRetries);
        backoff.setInitialInterval(initialRetryInterval);
        backoff.setMultiplier(backoffMultiplier);
        backoff.setMaxInterval(maxRetryInterval);
        var errorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer, backoff);
        var customExceptions = nonRetryableExceptions.stream()
                .filter(name -> !name.isBlank())
                .map(Classes::classWithName);
        var notRetryable = Streams.concat(DEFAULT_NOT_RETRYABLE.stream(), customExceptions)
                .toArray(Class[]::new);
        errorHandler.addNotRetryableExceptions(notRetryable);
        return errorHandler;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<?, ?> kafkaTemplate,
            @Value("${spring.application.name}") String applicationName,
            @Value("${prefab.dlt.topic.name:}") String dltTopicName
    ) {
        var dltTopic = !isEmpty(dltTopicName) ? dltTopicName : applicationName + ".dlt";
        return new DeadLetterPublishingRecoverer(
                Map.of(Object.class, kafkaTemplate),
                (record, ex) -> new TopicPartition(dltTopic, -1)
        );
    }
}
