package event.avro;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Transient;

@Event(topic = "transient-field", serialization = Event.Serialization.AVRO)
public record TransientFieldEvent(
        String id,
        @Transient String previewToken,
        Details details
) {
    public record Details(
            String sku,
            @Transient String cacheKey
    ) {
    }
}
