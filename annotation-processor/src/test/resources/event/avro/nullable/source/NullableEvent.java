package event.avro;

import be.appify.prefab.core.annotations.Event;
import jakarta.annotation.Nullable;

@Event(topic = "nullable", serialization = Event.Serialization.AVRO)
public record NullableEvent(
        String id,
        String name,
        @Nullable String description
) {
}