package sns.single.infrastructure.sns;

import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sns.single.UserCreated;
import sns.single.UserExporter;

@Component
public class UserExporterSqsSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserExporterSqsSubscriber.class);

    private final Executor executor;

    private final UserExporter userExporter;

    public UserExporterSqsSubscriber(UserExporter userExporter, SqsUtil sqsUtil) {
        executor = Executors.newFixedThreadPool(2);
        sqsUtil.subscribe(new SqsSubscriptionRequest<UserCreated>("user", "user-exporter-on-user-created", UserCreated.class, this::onUserCreated)
                .withExecutor(executor));
        this.userExporter = userExporter;
    }

    private void onUserCreated(UserCreated event) {
        log.debug("Received event {}", event);
        userExporter.onUserCreated(event);
    }
}
