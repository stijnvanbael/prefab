package sns.dltdisabled.infrastructure.sns;

import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sns.dltdisabled.UserEvent;
import sns.dltdisabled.UserExporter;

@Component
public class UserExporterSqsSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserExporterSqsSubscriber.class);

    private final Executor executor;

    private final UserExporter userExporter;

    public UserExporterSqsSubscriber(UserExporter userExporter, SqsUtil sqsUtil,
            @Value("${topic.user.name}") String userEventTopic) {
        executor = Executors.newFixedThreadPool(1);
        sqsUtil.subscribe(new SqsSubscriptionRequest<UserEvent>(userEventTopic, "user-exporter-on-user-event", UserEvent.class, this::onUserEvent)
                .withExecutor(executor)
                .withDeadLetterQueueName(null));
        this.userExporter = userExporter;
    }

    private void onUserEvent(UserEvent event) {
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
