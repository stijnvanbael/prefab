package be.appify.prefab.example.streams;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "${topics.streams.joined}", platform = Event.Platform.KAFKA)
public record JoinedStreamEvent(String id, String payload, String tag) {
}


