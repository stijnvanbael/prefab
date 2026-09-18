package event.avsc.invalidsynthetickeyreturn;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "invalid-synthetic-key-return", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/invalidsynthetickeyreturn/source/InvalidSyntheticKeyReturnEvent.avsc")
public interface InvalidSyntheticKeyReturnAvsc {
    String tenantId();

    String orderId();

    @PartitioningKey
    default TenantOrderKey tenantOrderKey() {
        return new TenantOrderKey(tenantId(), orderId());
    }

    record TenantOrderKey(String tenantId, String orderId) {
    }
}
