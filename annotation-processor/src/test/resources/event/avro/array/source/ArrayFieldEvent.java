package event.avro;

import be.appify.prefab.core.annotations.Event;
import java.util.List;

@Event(topic = "array-field", serialization = Event.Serialization.AVRO)
public record ArrayFieldEvent(
    List<String> tags,
    List<SaleLine> lines
) {
    public record SaleLine(
        String productId,
        int quantity
    ) {
    }
}