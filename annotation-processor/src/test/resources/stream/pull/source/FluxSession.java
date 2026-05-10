package stream.pull;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Streaming;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import reactor.core.publisher.Flux;

@Aggregate
@GetById
public record FluxSession(
        @Id String id,
        @Version long version,
        String title) {

    public FluxSession(String title) {
        this(UUID.randomUUID().toString(), 0L, title);
    }

    @Streaming(path = "/stream", event = "token")
    public Flux<String> streamTokens() {
        return Flux.just("hello", "world");
    }
}

