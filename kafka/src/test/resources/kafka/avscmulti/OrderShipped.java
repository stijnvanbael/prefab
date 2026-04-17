package kafka.avscmulti;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
@Event(topic = "prefab.order-shipped", platform = Event.Platform.KAFKA, serialization = Event.Serialization.AVRO)
@Avsc("kafka/avscmulti/OrderShippedEvent.avsc")
public interface OrderShipped {
}
