package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotate a class with one or more {@link EventHandler} methods to customize the behavior of the event handlers.
 */
@Target(ElementType.TYPE)
public @interface EventHandlerConfig {
    /** Default dead letter topic name. */
    String DEFAULT_DEAD_LETTER_TOPIC = "${prefab.dlt.topic.name}";

    /** Default retry limit. */
    String DEFAULT_RETRY_LIMIT = "${prefab.dlt.retries.limit:5}";

    /** Default minimum backoff in milliseconds. */
    String DEFAULT_MINIMUM_BACKOFF_MS = "${prefab.dlt.retries.minimum-backoff-ms:1000}";

    /** Default maximum backoff in milliseconds. */
    String DEFAULT_MAXIMUM_BACKOFF_MS = "${prefab.dlt.retries.maximum-backoff-ms:30000}";

    /** Default backoff multiplier. */
    String DEFAULT_BACKOFF_MULTIPLIER = "${prefab.dlt.retries.backoff-multiplier:1.5}";

    /** Default Kafka auto-offset-reset override (empty means use Spring/global defaults). */
    String DEFAULT_AUTO_OFFSET_RESET = "";

    /**
     * Configure the number of parallel threads to process events. Concurrency can either be a fixed number (e.g. "4") or a property
     * placeholder (e.g. "${event.handler.concurrency}"). Defaults to "1".
     *
     * @return The number of parallel threads to process events.
     */
    String concurrency() default "1";

    /**
     * Enable or disable dead lettering for the event handler. Defaults to true.
     *
     * @return true if dead lettering is enabled, false otherwise.
     */
    boolean deadLetteringEnabled() default true;

    /**
     * The dead letter topic to use for the event handler. Defaults to "${prefab.dlt.topic.name}".
     *
     * @return The dead letter topic.
     */
    String deadLetterTopic() default DEFAULT_DEAD_LETTER_TOPIC;

    /**
     * The retry limit for the event handler. Can either be a fixed number (e.g. "5") or a property placeholder
     * (e.g. "${event.handler.retry.limit}"). Defaults to "${prefab.dlt.retries.limit:5}".
     *
     * @return The retry limit.
     */
    String retryLimit() default DEFAULT_RETRY_LIMIT;

    /**
     * The minimum backoff time in milliseconds for retries. Can either be a fixed number (e.g. "1000") or a property
     * placeholder (e.g. "${event.handler.retries.minimum-backoff-ms}"). Defaults to "${prefab.dlt.retries.minimum-backoff-ms:1000}".
     *
     * @return The maximum backoff time in milliseconds.
     */
    String minimumBackoffMs() default DEFAULT_MINIMUM_BACKOFF_MS;

    /**
     * The maximum backoff time in milliseconds for retries. Can either be a fixed number (e.g. "30000") or a property
     * placeholder (e.g. "${event.handler.retries.maximum-backoff-ms}"). Defaults to "${prefab.dlt.retries.maximum-backoff-ms:30000}".
     *
     * @return The maximum backoff time in milliseconds.
     */
    String maximumBackoffMs() default DEFAULT_MAXIMUM_BACKOFF_MS;

    /**
     * The backoff multiplier for retries. Can either be a fixed number (e.g. "1.5") or a property
     * placeholder (e.g. "${event.handler.retries.backoff-multiplier}"). Defaults to "${prefab.dlt.retries.backoff-multiplier:1.5}".
     *
     * @return The backoff multiplier.
     */
    String backoffMultiplier() default "${prefab.dlt.retries.backoff-multiplier:1.5}";

    /**
     * Kafka-only override for consumer {@code auto.offset.reset}. Accepts either a literal
     * value such as {@code earliest}/{@code latest} or a Spring property placeholder.
     * Leave empty to use framework defaults.
     *
     * @return Kafka auto-offset-reset override or empty string.
     */
    String autoOffsetReset() default DEFAULT_AUTO_OFFSET_RESET;

    /**
     * Restricts which topics this handler subscribes to. When non-empty, the generated listener
     * will only subscribe to the topics listed here; they must be a subset of the topics declared
     * on the corresponding {@link Event}. When empty (the default), the handler subscribes to
     * <em>all</em> topics declared on the event.
     *
     * @return the topics this handler should consume from, or an empty array to consume from all event topics.
     */
    String[] consumeFromTopics() default {};

    /**
     * Utility interface providing helper methods to evaluate configuration properties
     * defined in the {@link EventHandlerConfig}.
     */
    interface Util {

        /**
         * Determines whether the provided {@link EventHandlerConfig} has a custom configuration. A configuration
         * is considered custom if dead lettering is disabled or the dead letter topic is not equal to the default topic.
         *
         * @param config The {@link EventHandlerConfig} instance to evaluate.
         * @return {@code true} if the configuration is custom, {@code false} otherwise.
         */
        static boolean hasCustomConfig(EventHandlerConfig config) {
            return config != null
                    && (!config.deadLetteringEnabled() || !DEFAULT_DEAD_LETTER_TOPIC.equals(config.deadLetterTopic()));
        }

        /**
         * Determines if the provided {@link EventHandlerConfig} has a custom dead letter topic.
         * A configuration is considered to have a custom dead letter topic if dead lettering is enabled
         * and the dead letter topic is not the default topic.
         *
         * @param config The {@link EventHandlerConfig} to evaluate.
         * @return {@code true} if the dead letter topic is custom, {@code false} otherwise.
         */
        static boolean hasCustomDeadLetterTopic(EventHandlerConfig config) {
            return config != null && config.deadLetteringEnabled() && !DEFAULT_DEAD_LETTER_TOPIC.equals(config.deadLetterTopic());
        }

        /**
         * Determines whether the provided {@link EventHandlerConfig} has custom retry settings.
         * A configuration is considered to have custom retries if any of the retry-related properties
         * differ from their default values.
         *
         * @param config The {@link EventHandlerConfig} instance to evaluate.
         * @return {@code true} if custom retry settings are present, {@code false} otherwise.
         */
        static boolean hasCustomRetries(EventHandlerConfig config) {
            return config != null && (
                    !DEFAULT_RETRY_LIMIT.equals(config.retryLimit())
                    || !DEFAULT_MINIMUM_BACKOFF_MS.equals(config.minimumBackoffMs())
                    || !DEFAULT_MAXIMUM_BACKOFF_MS.equals(config.maximumBackoffMs())
                    || !DEFAULT_BACKOFF_MULTIPLIER.equals(config.backoffMultiplier()));
        }

        /**
         * Determines whether the provided {@link EventHandlerConfig} explicitly overrides Kafka
         * {@code auto.offset.reset}.
         *
         * @param config The {@link EventHandlerConfig} instance to evaluate.
         * @return {@code true} if a non-empty override is provided, {@code false} otherwise.
         */
        static boolean hasCustomAutoOffsetReset(EventHandlerConfig config) {
            return config != null && !config.autoOffsetReset().isBlank();
        }

        /**
         * Determines whether the provided {@link EventHandlerConfig} restricts which topics are consumed.
         * Returns {@code true} when the {@code consumeFromTopics} array is non-empty.
         *
         * @param config The {@link EventHandlerConfig} instance to evaluate.
         * @return {@code true} if a topic filter is specified, {@code false} otherwise.
         */
        static boolean hasConsumeFromTopicsFilter(EventHandlerConfig config) {
            return config != null && config.consumeFromTopics().length > 0;
        }
    }
}
