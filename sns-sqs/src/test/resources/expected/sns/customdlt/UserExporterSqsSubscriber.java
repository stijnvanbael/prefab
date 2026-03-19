package sns.customdlt.infrastructure.sns;

import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import sns.customdlt.UserEvent;
import sns.customdlt.UserExporter;

@Component
public class UserExporterSqsSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserExporterSqsSubscriber.class);

    private final Executor executor;

    private final UserExporter userExporter;

    public UserExporterSqsSubscriber(UserExporter userExporter, SqsUtil sqsUtil,
            @Value("${prefab.dlt.retries.backoff-multiplier:1.5}") Double backoffMultiplier,
            @Value("${topic.user.name}") String userEventTopic,
            @Value("${custom.dlt.name}") String deadLetterTopic) {
        executor = Executors.newFixedThreadPool(1);
        sqsUtil.subscribe(new SqsSubscriptionRequest<UserEvent>(userEventTopic, "user-exporter-on-user-event", UserEvent.class, this::onUserEvent)
                .withExecutor(executor)
                .withDeadLetterQueueName(deadLetterTopic)
                .withRetryTemplate(new RetryTemplate(RetryPolicy.builder()
                        .maxRetries(10)
                        .delay(Duration.ofMillis(100L))
                        .maxDelay(Duration.ofMillis(10000L))
                        .multiplier(backoffMultiplier)
                        .build())));
        this.userExporter = userExporter;
    }

    private void onUserEvent(UserEvent event) {
        log.debug("Received event {}", event);
        switch (event) {
            case UserEvent.Created e -> userExporter.onUserCreated(e);
            case UserEvent.Updated e -> userExporter.onUserUpdated(e);
            case UserEvent.Deleted e -> userExporter.onUserDeleted(e);
            default -> {
            }
        }
    }
}
