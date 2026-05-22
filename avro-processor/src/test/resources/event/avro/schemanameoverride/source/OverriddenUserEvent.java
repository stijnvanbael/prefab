package event.avro;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "users")
public interface OverriddenUserEvent {
	String id();
}


