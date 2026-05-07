package be.appify.prefab.example.mongodb.streamsession;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Streaming;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/**
 * Demonstrates the pull-model SSE streaming feature. Each session stores a fixed list of messages
 * and exposes a streaming endpoint that pushes them to connected SSE clients.
 */
@Aggregate
@GetById
public record StreamSession(
        @Id Reference<StreamSession> id,
        @Version long version,
        List<String> messages
) {

    /** Creates a new streaming session pre-loaded with the given messages. */
    @Create
    public StreamSession(@NotNull List<String> messages) {
        this(Reference.create(), 0L, List.copyOf(messages));
    }

    /** Streams all stored messages to the SSE client. Heartbeat every 1 s keeps proxies alive. */
    @Streaming(path = "/stream", event = "message", heartbeatSeconds = 1)
    public Stream<String> streamMessages() {
        return messages.stream();
    }
}

