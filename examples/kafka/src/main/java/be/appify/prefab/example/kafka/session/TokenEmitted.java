package be.appify.prefab.example.kafka.session;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

/**
 * Domain event emitted when a token (e.g. an LLM output chunk) is ready for a chat session.
 * The {@code done} flag signals the final token of a response; when set to {@code true}
 * the SSE stream for that session is closed automatically.
 */
@Event(topic = "${topics.token.name}")
public record TokenEmitted(
        @PartitioningKey Reference<ChatSession> sessionId,
        String token,
        boolean done
) {
}

