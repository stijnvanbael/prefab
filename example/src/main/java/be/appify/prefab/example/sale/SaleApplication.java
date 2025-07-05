package be.appify.prefab.example.sale;

import be.appify.prefab.processor.kafka.KafkaConfiguration;
import be.appify.prefab.processor.pubsub.PubSubConfiguration;
import be.appify.prefab.processor.spring.PrefabConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication
// TODO: Make sure all of this is imported automatically by the prefab starter
@Import({PrefabConfiguration.class, KafkaConfiguration.class, PubSubConfiguration.class, GcpPubSubAutoConfiguration.class, GcpContextAutoConfiguration.class})
public class SaleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SaleApplication.class, args);
    }
}
