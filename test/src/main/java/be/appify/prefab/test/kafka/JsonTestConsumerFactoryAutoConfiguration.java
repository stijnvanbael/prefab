package be.appify.prefab.test.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonTypeResolver;

@Configuration
@ConditionalOnClass(JsonDeserializer.class)
public class JsonTestConsumerFactoryAutoConfiguration {

    private final KafkaProperties properties;

    public JsonTestConsumerFactoryAutoConfiguration(KafkaProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(name = "jsonTestConsumerFactory")
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public DefaultKafkaConsumerFactory<String, Object> jsonTestConsumerFactory(
            KafkaConnectionDetails connectionDetails,
            ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers, ObjectProvider<SslBundles> sslBundles,
            ObjectMapper objectMapper,
            TestJsonTypeResolver jsonTypeResolver
    ) {
        var consumerProperties = this.properties.buildConsumerProperties(sslBundles.getIfAvailable());
        consumerProperties.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getConsumer().getBootstrapServers());
        var factory = new DefaultKafkaConsumerFactory<String, Object>(consumerProperties);
        try (var jsonDeserializer = new JsonDeserializer<>(objectMapper)) {
            jsonDeserializer.typeResolver(jsonTypeResolver::resolveType);
            factory.setValueDeserializer(jsonDeserializer);
        }
        customizers.orderedStream().forEach(customizer -> customizer.customize(factory));
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(name = "testJsonTypeResolver")
    public TestJsonTypeResolver testJsonTypeResolver(ObjectProvider<JsonTypeResolver> delegate) {
        return new TestJsonTypeResolver(delegate.getIfAvailable(() -> (topic, data, headers) -> {
            throw new IllegalStateException("No type resolver configured for topic: " + topic);
        }));
    }
}
