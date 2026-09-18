package kafka.avscsynthetic;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "prefab.synthetic.avsc", platform = Event.Platform.KAFKA, serialization = Event.Serialization.AVRO)
@Avsc("kafka/avscsynthetic/SyntheticOrderCreated.avsc")
public interface SyntheticOrderEvents {
    String tenantId();

    String orderId();

    @PartitioningKey
    default String tenantOrderKey() {
        return tenantId() + ":" + orderId();
    }
}
