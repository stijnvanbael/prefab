package kafka.offsetoverride.infrastructure.kafka;

import kafka.offsetoverride.UserCreated;
import kafka.offsetoverride.UserExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserExporterKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(UserExporterKafkaConsumer.class);

    private final UserExporter userExporter;

    public UserExporterKafkaConsumer(UserExporter userExporter) {
        this.userExporter = userExporter;
    }

    @KafkaListener(
            topics = "prefab.user",
            groupId = "${spring.application.name}.user-exporter-on-user-created",
            concurrency = "1",
            properties = "auto.offset.reset=${offset.override:latest}"
    )
    public void onUserCreated(UserCreated event) {
        log.debug("Received event {}", event);
        userExporter.onUserCreated(event);
    }
}

