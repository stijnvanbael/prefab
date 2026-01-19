package pubsub.customdlt;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

@Aggregate
public record User(
        @Id String id,
        @Version long version,
        String name
) {
    @PersistenceCreator
    public User {
    }

    @Create
    public User(String name) {
        this(UUID.randomUUID().toString(), 0L, name);
    }
}
