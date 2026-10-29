package event.avro;

import be.appify.prefab.core.annotations.Default;
import be.appify.prefab.core.annotations.Event;
import jakarta.annotation.Nullable;

@Event(topic = "nullable-default", serialization = Event.Serialization.AVRO)
public record NullableDefaultEvent(@Nullable @Default("x") String name) {
}
