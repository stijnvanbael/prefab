package be.appify.prefab.example.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;

/** A single word extracted from an incoming {@link StreamEvent} payload. */
@Event(topic = "${topics.streams.words}", platform = Event.Platform.KAFKA)
public record WordEvent(String id, Word word) implements Keyed<Word> {
    @Override
    public Word key() {
        return word;
    }
}

