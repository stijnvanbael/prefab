package stream.pull;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.GetById;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Session(
        @Id String id,
        @Version long version,
        String title) {

    public Session(String title) {
        this(UUID.randomUUID().toString(), 0L, title);
    }

    @be.appify.prefab.core.annotations.rest.Streaming(path = "/stream", event = "token")
    public java.util.stream.Stream<String> streamTokens() {
        return java.util.stream.Stream.of("hello", "world");
    }
}


