package event.avro;

import be.appify.prefab.core.annotations.Default;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PreviousName;
import java.util.List;

@Event(topic = "evolved", serialization = Event.Serialization.AVRO)
@PreviousName({"LegacyEvolved", "legacy.OldEvolved", "Not.Valid$Name"})
public record EvolvedEvent(
        @PreviousName({"fullName", "title"}) String name,
        @Default("unknown") String label,
        @Default("7") int count,
        @Default("2") Long total,
        @Default("1.5") double ratio,
        @Default("0.5") float factor,
        @Default("true") boolean active,
        @Default("PENDING") Status status,
        @Default("[]") List<String> tags
) {
    public enum Status {PENDING, ACTIVE}
}
