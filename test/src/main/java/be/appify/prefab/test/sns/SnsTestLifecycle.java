package be.appify.prefab.test.sns;

import be.appify.prefab.core.sns.SqsUtil;
import javax.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Lifecycle management for SNS/SQS tests.
 */
@Component
@ConditionalOnBean(SqsUtil.class)
public class SnsTestLifecycle {
    private final SqsUtil sqsUtil;

    /**
     * Constructs a new SnsTestLifecycle.
     *
     * @param sqsUtil the SQS utility
     */
    public SnsTestLifecycle(SqsUtil sqsUtil) {
        this.sqsUtil = sqsUtil;
    }

    /**
     * Cleans up all SNS topics and SQS queues when the context is destroyed.
     */
    @PreDestroy
    public void cleanUp() {
        sqsUtil.deleteAllQueues();
        sqsUtil.deleteAllTopics();
    }
}
