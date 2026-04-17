package kafka.avscmulti;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
@Event(topic = "prefab.order", platform = Event.Platform.KAFKA, serialization = Event.Serialization.AVRO)
@Avsc("kafka/avscmulti/OrderCreatedEvent.avsc")
public interface OrderCreated {
}
