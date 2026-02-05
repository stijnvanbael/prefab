package be.appify.prefab.example.avro.customer;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.domain.PublishesEvents;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Embedded;

@Aggregate
@DbMigration
public record Customer(
        @Id String id,
        @Version long version,
        @NotNull @Embedded.Nullable(prefix = "name_") PersonName name,
        @NotNull String email
) implements PublishesEvents {
    @PersistenceCreator
    public Customer {
    }

    @Create
    public Customer(@NotNull PersonName name, @NotNull String email) {
        this(UUID.randomUUID().toString(), 0L, name, email);
        publish(new Created(id, name, email));
    }

    @Delete
    public void delete() {
        publish(new Deleted(id));
    }

    @Event(topic = "customer", serialization = Event.Serialization.AVRO)
    public sealed interface Events permits Created, Deleted {
        @PartitioningKey
        String id();
    }

    public record Created(String id, PersonName name, String email) implements Events {
    }

    public record Deleted(String id) implements Events {
    }
}
