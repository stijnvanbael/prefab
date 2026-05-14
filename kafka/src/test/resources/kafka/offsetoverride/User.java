package kafka.offsetoverride;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record User(
        @Id String id,
        @Version long version,
        String username
) {
    @Create
    public User(String username) {
        this(UUID.randomUUID().toString(), 0L, username);
    }
}

