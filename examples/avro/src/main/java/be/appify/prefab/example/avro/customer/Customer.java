package be.appify.prefab.example.avro.customer;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record Customer(
        @Id Reference<Customer> id,
        @Version long version,
        PersonName name,
        String email
) implements PublishesEvents {

    @Create
    public Customer(@NotNull PersonName name, @NotNull String email) {
        this(Reference.create(), 0L, name, email);
        publish(new Created(id, name, email));
    }

    @Delete
    public void delete() {
        publish(new Deleted(id));
    }

    @Event(topic = "customer", serialization = Event.Serialization.AVRO)
    public sealed interface Events permits Created, Deleted {
        @PartitioningKey
        Reference<Customer> id();
    }

    public record Created(Reference<Customer> id, PersonName name, String email) implements Events {
    }

    public record Deleted(Reference<Customer> id) implements Events {
    }
}
