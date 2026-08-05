package event.avro;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "nested-decimal", serialization = Event.Serialization.AVRO)
@Avsc("event/avro/nesteddecimal/source/NestedDecimalEvent.avsc")
public interface NestedDecimalContract {
}
