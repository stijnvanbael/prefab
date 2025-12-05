package be.appify.prefab.example.kafka.channel;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.domain.PublishesEvents;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

import java.util.UUID;

@Aggregate
@GetList
@DbMigration
public final class Channel implements PublishesEvents {
    @Id
    private final String id;
    @Version
    private final long version;
    @NotNull
    private final String name;

    @PersistenceCreator
    public Channel(String id, long version, @NotNull String name) {
        this.id = id;
        this.version = version;
        this.name = name;
    }

    @Create
    public Channel(String name) {
        this(UUID.randomUUID().toString(), 0L, name);
    }

    public String id() {
        return id;
    }

    public long version() {
        return version;
    }

    public String name() {
        return name;
    }
}
