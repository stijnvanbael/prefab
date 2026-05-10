package stream.push;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "tokens")
public record TokenEmitted(
        String sessionId,
        String token,
        boolean done) {
}

