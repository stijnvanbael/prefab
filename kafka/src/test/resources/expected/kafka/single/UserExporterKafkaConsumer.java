package kafka.single.infrastructure.kafka;

import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.single.UserCreated;
import kafka.single.UserExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserExporterKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(UserExporterKafkaConsumer.class);

    private final UserExporter userExporter;

    public UserExporterKafkaConsumer(UserExporter userExporter,
            KafkaJsonTypeResolver typeResolver) {
        typeResolver.registerType("prefab.user", UserCreated.class);
        this.userExporter = userExporter;
    }

    @KafkaListener(
            topics = "prefab.user",
            groupId = "${spring.application.name}.user-exporter-on-user-created",
            concurrency = "2"
    )
    public void onUserCreated(UserCreated event) {
        log.debug("Received event {}", event);
        userExporter.onUserCreated(event);
    }
}
