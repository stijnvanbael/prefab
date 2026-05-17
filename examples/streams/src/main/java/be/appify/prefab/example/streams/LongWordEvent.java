package be.appify.prefab.example.streams;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "${topics.streams.long-words}", platform = Event.Platform.KAFKA)
public record LongWordEvent(String id, String word) {
}

