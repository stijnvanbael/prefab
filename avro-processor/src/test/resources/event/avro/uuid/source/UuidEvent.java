package event.avro;

import be.appify.prefab.core.annotations.Event;
import jakarta.annotation.Nullable;
import java.util.UUID;

@Event(topic = "uuid", serialization = Event.Serialization.AVRO)
public record UuidEvent(
        UUID id,
        @Nullable UUID parentId
) {
}
