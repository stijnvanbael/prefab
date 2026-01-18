package be.appify.prefab.core.kafka;

import java.util.List;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * <p>Utility class that provides constants and configurations for Kafka-related operations.
 * This class is primarily designed to contain shared logic and reusable Kafka-specific
 * properties, such as default exception handling configurations.</p>
 *
 * <p>The class includes a predefined list of exceptions that are considered non-retryable
 * in Kafka-related operations.</p>
 */
public class KafkaUtil {
    private KafkaUtil() {
    }

    /**
     * <p>A predefined unmodifiable list of exception types that are considered non-retryable in Kafka-related operations.
     * These exceptions typically represent errors that cannot be resolved by retrying the operation, such as
     * serialization issues, invalid data, or incorrect configurations.</p>
     *
     * <p>This list is intended to be used as a reference for exception handling logic, particularly in scenarios
     * where retries need to be selectively applied based on the nature of the exception encountered.</p>
     */
    public static final List<Class<? extends Exception>> DEFAULT_NOT_RETRYABLE = List.of(
            SerializationException.class,
            NullPointerException.class,
            IllegalArgumentException.class,
            IllegalStateException.class,
            RecordDeserializationException.class,
            InvalidTopicException.class,
            DataIntegrityViolationException.class);
}
