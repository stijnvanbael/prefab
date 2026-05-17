package be.appify.prefab.example.streams;

import be.appify.prefab.core.annotations.Event;

/** A single word extracted from an incoming {@link StreamEvent} payload. */
@Event(topic = "${topics.streams.words}", platform = Event.Platform.KAFKA)
public record WordEvent(String id, String word) {
}

