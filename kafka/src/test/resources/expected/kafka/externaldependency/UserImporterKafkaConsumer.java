package kafka.externaldependency.infrastructure.kafka;

import kafka.dependencyevents.ExternalUserCreated;
import kafka.externaldependency.UserImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserImporterKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(UserImporterKafkaConsumer.class);

    private final UserImporter userImporter;

    public UserImporterKafkaConsumer(UserImporter userImporter) {
        this.userImporter = userImporter;
    }

    @KafkaListener(
            topics = "prefab.external.user",
            groupId = "${spring.application.name}.user-importer-on-external-user-created",
            concurrency = "2"
    )
    public void onExternalUserCreated(ExternalUserCreated event) {
        log.debug("Received event {}", event);
        userImporter.onExternalUserCreated(event);
    }
}

