package be.appify.prefab.example.streams;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "${topics.streams.input}", platform = Event.Platform.KAFKA)
public record StreamEvent(String id, String payload) {
}

