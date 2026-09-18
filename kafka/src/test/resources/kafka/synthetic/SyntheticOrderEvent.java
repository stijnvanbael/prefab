package kafka.synthetic;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "prefab.synthetic", platform = Event.Platform.KAFKA)
public record SyntheticOrderEvent(String tenantId, String orderId) {
    @PartitioningKey
    public String tenantOrderKey() {
        return tenantId + ":" + orderId;
    }
}
