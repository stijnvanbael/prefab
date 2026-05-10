package be.appify.prefab.example.kafka.session;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Streaming;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/**
 * Represents a chat session aggregate. Demonstrates the push-model SSE streaming pattern:
 * a Kafka {@code TokenEmitted} event is forwarded to any connected SSE client in real time.
 */
@Aggregate
@GetById
public record ChatSession(
        @Id Reference<ChatSession> id,
        @Version long version,
        String title,
        boolean completed
) {

    /** Creates a new chat session with the given title. */
    @Create
    public ChatSession(@NotBlank String title) {
        this(Reference.create(), 0L, title, false);
    }

    /**
     * Handles a token event: updates completion state and pushes the token to connected SSE clients.
     * The {@code terminal = "done"} attribute closes the SSE stream when the final token arrives.
     */
    @EventHandler
    @ByReference(property = "sessionId")
    @Streaming(path = "/stream", event = "token", terminal = "done", heartbeatSeconds = 1)
    public ChatSession onTokenEmitted(TokenEmitted event) {
        return new ChatSession(id, version, title, event.done());
    }
}

