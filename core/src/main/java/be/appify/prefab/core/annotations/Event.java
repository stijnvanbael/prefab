package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Annotate an event class to specify its messaging topic, platform, publisher, and serialization format.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Event {
    /// The topic the event is published to or should be consumed from.
    String topic();

    /// The messaging platform used for the event.
    Platform platform() default Platform.KAFKA;

    /// The aggregate root class that publishes this event.
    Class<?> publishedBy();

    /// The serialization format used for the event.
    Serialization serialization() default Serialization.JSON;

    enum Platform {
        KAFKA,
        PUB_SUB,
    }

    enum Serialization {
        JSON,
    }
}
