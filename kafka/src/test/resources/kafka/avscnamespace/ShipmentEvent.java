package kafka.avscnamespace;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "prefab.shipment", platform = Event.Platform.KAFKA, serialization = Event.Serialization.AVRO)
@Avsc("kafka/avscnamespace/ShipmentDispatched.avsc")
public interface ShipmentEvent {
    @PartitioningKey
    String shipmentId();
}
