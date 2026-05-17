package be.appify.prefab.example.streams;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "${topics.streams.short-words}", platform = Event.Platform.KAFKA)
public record ShortWordEvent(String id, String word) implements ClassifiedWordEvent {
}

