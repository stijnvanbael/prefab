package be.appify.prefab.processor.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonTypeResolver;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import java.time.Duration;
import java.util.UUID;

@Configuration
@ComponentScan(basePackageClasses = KafkaJsonTypeResolver.class)
public class KafkaConfiguration {
    @Bean
    @ConditionalOnMissingBean(ProducerFactory.class)
    public ProducerFactory<Object, Object> kafkaProducerFactory(
            KafkaProperties kafkaProperties,
            KafkaConnectionDetails connectionDetails,
            ObjectProvider<DefaultKafkaProducerFactoryCustomizer> customizers,
            ObjectProvider<SslBundles> sslBundles,
            @Value("${spring.application.name}") String applicationName,
            @Value("${kafka.transactions.enabled:true}") boolean transactionsEnabled
    ) {
        var properties = kafkaProperties.buildProducerProperties(sslBundles.getIfAvailable());
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
    public ConsumerFactory<?, ?> kafkaConsumerFactory(
            KafkaProperties kafkaProperties,
            KafkaConnectionDetails connectionDetails,
            ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers,
            ObjectProvider<KafkaTransactionManager<?, ?>> kafkaTransactionManager,
            ObjectProvider<SslBundles> sslBundles,
            JsonTypeResolver jsonTypeResolver,
            @Value("${spring.application.name}") String applicationName
    ) {
        var properties = kafkaProperties.buildConsumerProperties(sslBundles.getIfAvailable());
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getConsumer().getBootstrapServers());
        properties.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, applicationName);
        kafkaTransactionManager.ifAvailable(ignored ->
                properties.putIfAbsent(ConsumerConfig.ISOLATION_LEVEL_CONFIG, KafkaProperties.IsolationLevel.READ_COMMITTED.name()));
        var jsonDeserializer = new JsonDeserializer<>();
        jsonDeserializer.setTypeResolver(jsonTypeResolver);
        var deserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);
        deserializer.configure(properties, false);
        var factory = new DefaultKafkaConsumerFactory<>(properties, new StringDeserializer(), deserializer);
        customizers.orderedStream().forEach(customizer -> customizer.customize(factory));
        return factory;
    }
}
