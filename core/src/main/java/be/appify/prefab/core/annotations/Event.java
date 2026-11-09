package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Repeatable;

/** Annotate an event class to specify its messaging topic, platform, producer, and serialization format. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Events.class)
public @interface Event {
    /**
     * Concrete class arguments bound positionally to the event's type parameters.
     *
     * @return the concrete arguments; empty for non-generic events
     */
    Class<?>[] typeArguments() default {};

    /**
     * The topic(s) the event is published to or should be consumed from.
     *
     * @return The topic(s) the event is published to or should be consumed from.
     */
    String[] topic();

    /**
     * Controls which of the registered topics this event is published to.
     * Defaults to {@link PublishTo#FIRST} for backward compatibility.
     *
     * @return the publish-to strategy
     */
    PublishTo publishTo() default PublishTo.FIRST;

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

    /**
     * The semantic version of the event contract. Producers stamp it in the {@code prefab-event-version} header.
     * PATCH: no contract change. MINOR: backward-compatible change, no migration. MAJOR: breaking change, a
     * {@link EventMigration} is required.
     *
     * @return the version, {@code major.minor.patch}
     */
    String version() default "1.0.0";

    /**
     * The compatibility mode the annotation processor enforces against the committed baseline contract.
     *
     * @return the compatibility mode
     */
    Compatibility compatibility() default Compatibility.DEFAULT;

    /**
     * The oldest version that is still supported. Messages of an older major version are rejected explicitly.
     * Empty means that every version for which a migration chain exists is supported.
     *
     * @return the minimum supported version, empty if not restricted
     */
    String minSupportedVersion() default "";

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
        /** AWS SNS/SQS messaging platform. */
        SNS_SQS {
            @Override
            public String toString() {
                return "SNS/SQS";
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
