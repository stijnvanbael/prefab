package be.appify.prefab.example.showcase.streams.kafka;

import be.appify.prefab.example.showcase.streams.CatalogProduct;
import be.appify.prefab.streams.kafka.KafkaStreamBackend;
import java.util.Optional;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provisions the source topic required to initialize the catalog global store. */
@Configuration
@ConditionalOnClass(KafkaStreamBackend.class)
class KafkaCatalogProductTopicConfiguration {
    @Bean
    NewTopic catalogProductTopic() {
        return new NewTopic(CatalogProduct.TOPIC, Optional.empty(), Optional.empty());
    }
}
