package stream.push;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.GetById;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record ChatSession(
        @Id String id,
        @Version long version,
        String title) {

    public ChatSession(String title) {
        this(UUID.randomUUID().toString(), 0L, title);
    }

    @EventHandler
    public static ChatSession onCreate(ChatSession event) {
        return event;
    }

    @EventHandler
    @ByReference(property = "sessionId")
    @be.appify.prefab.core.annotations.rest.Streaming(path = "/stream", event = "token", terminal = "done")
    public ChatSession onTokenEmitted(TokenEmitted event) {
        return new ChatSession(id, version, title);
    }
}


