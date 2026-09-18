package event.avsc.invalidsynthetickeydependency;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "invalid-synthetic-key-dependency", serialization = Event.Serialization.AVRO)
@Avsc({
        "event/avsc/invalidsynthetickeydependency/source/InvalidSyntheticKeyDependencyEventA.avsc",
        "event/avsc/invalidsynthetickeydependency/source/InvalidSyntheticKeyDependencyEventB.avsc"
})
public interface InvalidSyntheticKeyDependencyAvsc {
    String tenantId();

    String orderId();

    @PartitioningKey
    default String tenantOrderKey() {
        return tenantId() + ":" + orderId();
    }
}
