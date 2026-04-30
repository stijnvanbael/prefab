package event.avro;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.service.Reference;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Event(topic = "non-primitive", serialization = Event.Serialization.AVRO)
public record NonPrimitiveEvent(
        Status status,
        Instant timestamp,
        LocalDate date,
        Duration duration,
        Reference<Object> reference
) {
    public enum Status {
        ACTIVE,
        INACTIVE,
        PENDING
    }
}