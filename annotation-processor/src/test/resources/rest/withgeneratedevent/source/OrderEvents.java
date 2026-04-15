package rest.withgeneratedevent;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "order-created", serialization = Event.Serialization.AVRO)
@Avsc("rest/withgeneratedevent/source/OrderCreatedEvent.avsc")
public interface OrderEvents {
}

