package event.avsc.synthetic;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "synthetic-key", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/synthetickey/source/SyntheticKeyAvscEvent.avsc")
public interface SyntheticKeyAvsc {
    String tenantId();

    String orderId();

    @PartitioningKey
    default String tenantOrderKey() {
        return tenantId() + ":" + orderId();
    }
}
