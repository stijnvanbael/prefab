package be.appify.prefab.example.sale;

import be.appify.prefab.processor.kafka.KafkaConfiguration;
import be.appify.prefab.processor.spring.PrefabConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
// TODO: Make sure all of this is imported automatically by the prefab starter
@Import({PrefabConfiguration.class, KafkaConfiguration.class})
public class SaleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SaleApplication.class, args);
    }
}
