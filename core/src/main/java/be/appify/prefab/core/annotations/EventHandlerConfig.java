package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotate a class with one or more {@link EventHandler} methods to customize the behavior of the event handlers.
 */
@Target(ElementType.TYPE)
public @interface EventHandlerConfig {

    String DEFAULT_DLT_TOPIC = "${prefab.dlt.topic.name}";

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
    String deadLetterTopic() default DEFAULT_DLT_TOPIC;

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
            return !config.deadLetteringEnabled() || !DEFAULT_DLT_TOPIC.equals(config.deadLetterTopic());
        }

        /**
         * Determines if the provided {@link EventHandlerConfig} has a custom dead letter topic.
         * A configuration is considered to have a custom dead letter topic if dead lettering is enabled
         * and the dead letter topic is not the default topic.
         *
         * @param config The {@link EventHandlerConfig} to evaluate.
         * @return {@code true} if the dead letter topic is custom, {@code false} otherwise.
         */
        static boolean hasCustomDltTopic(EventHandlerConfig config) {
            return config.deadLetteringEnabled() && !DEFAULT_DLT_TOPIC.equals(config.deadLetterTopic());
        }
    }
}
