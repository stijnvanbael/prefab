package kafka.avscpartial;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
@Event(topic = "prefab.order", platform = Event.Platform.KAFKA, serialization = Event.Serialization.AVRO)
@Avsc({"kafka/avscpartial/OrderCreatedEvent.avsc", "kafka/avscpartial/OrderShippedEvent.avsc"})
public interface OrderEvent {
}
