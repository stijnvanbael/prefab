package event.avro;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PreviousName;

@Event(topic = "collision", serialization = Event.Serialization.AVRO)
public record CollisionEvent(@PreviousName("age") String name, int age, @PreviousName("x") String a, @PreviousName("x") String b) {
}
