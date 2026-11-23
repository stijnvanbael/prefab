package be.appify.prefab.streams.snssqs;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration of the SNS/SQS streams backend.
 *
 * @param visibilityTimeout visibility timeout of the queues of source nodes. It must exceed the time to process one
 *         message, including the engine's conflict retries and the retries of the dead-letter policy, otherwise SQS
 *         delivers the message again while it is still being processed
 */
@ConfigurationProperties("prefab.streams.sns-sqs")
public record SnsSqsStreamsProperties(@DefaultValue("PT2M") Duration visibilityTimeout) {
    /** Validates the settings. */
    public SnsSqsStreamsProperties {
        if (visibilityTimeout.toSeconds() < 1 || visibilityTimeout.toHours() > 12) {
            throw new IllegalArgumentException(
                    "prefab.streams.sns-sqs.visibility-timeout must be between one second and twelve hours");
        }
    }
}
