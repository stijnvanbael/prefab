package kafka.multiple.infrastructure.kafka;

import kafka.multiple.UserEvent;
import kafka.multiple.UserExporter;
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
            topics = "${topic.user.name}",
            groupId = "${spring.application.name}.user-exporter-on-user-event",
            concurrency = "${user-exporter.concurrency:4}"
    )
    public void onUserEvent(UserEvent event) {
        log.debug("Received event {}", event);
        switch (event) {
            case UserEvent.Created e -> userExporter.onUserCreated(e);
            case UserEvent.Deleted e -> userExporter.onUserDeleted(e);
            case UserEvent.Updated e -> userExporter.onUserUpdated(e);
            default -> {
            }
        }
    }
}
