package stream.pull;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Streaming;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record RecordSession(
        @Id String id,
        @Version long version,
        String title) {

    public RecordSession(String title) {
        this(UUID.randomUUID().toString(), 0L, title);
    }

    @Streaming(path = "/tokens", event = "token")
    public Stream<TokenItem> streamTokenItems() {
        return List.of(new TokenItem("hello"), new TokenItem("world")).stream();
    }
}

