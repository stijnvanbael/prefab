package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotate an event class to specify its messaging topic, platform, producer, and serialization format. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Event {
    /**
     * The topic the event is published to or should be consumed from.
     *
     * @return The topic the event is published to or should be consumed from.
     */
    String topic();

    /**
     * The messaging platform used for the event. The default is to derive the platform from the application
     * configuration.
     *
     * @return The messaging platform used for the event.
     */
    Platform platform() default Platform.DERIVED;

    /**
     * The serialization format used for the event.
     *
     * @return The serialization format used for the event.
     */
    Serialization serialization() default Serialization.JSON;

    /** The supported messaging platforms. */
    enum Platform {
        /**
         * Derive the platform from the from application configuration. The platform can only be derived if there is a
         * single messaging platform configured. If not, an error will be raised during code generation.
         */
        DERIVED,
        /** Kafka messaging platform. */
        KAFKA {
            @Override
            public String toString() {
                return "Kafka";
            }
        },
        /** Pub/Sub messaging platform. */
        PUB_SUB {
            @Override
            public String toString() {
                return "Pub/Sub";
            }
        },
    }

    /** The supported serialization formats. */
    enum Serialization {
        /** JSON serialization format. */
        JSON,

        /** AVRO serialization format. */
        AVRO
    }
}
