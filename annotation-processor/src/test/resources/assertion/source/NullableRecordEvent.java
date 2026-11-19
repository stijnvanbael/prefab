package assertion;

import be.appify.prefab.core.annotations.Event;
import org.jspecify.annotations.Nullable;

@Event(topic = "nullable-record-event", serialization = Event.Serialization.JSON)
public record NullableRecordEvent(@Nullable Payload payload) {
    public record Payload(String code) {
    }
}
