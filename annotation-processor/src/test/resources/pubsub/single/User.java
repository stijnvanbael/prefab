package pubsub.single;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

import java.util.UUID;

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
