package event.serialization.nested.sale;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Sale(
        @Id Reference<Sale> id,
        @Version long version
) implements PublishesEvents {

    @Create
    public Sale() {
        this(Reference.create(), 0L);
        publish(new Created(id));
    }

    @Event(topic = "sale", serialization = Event.Serialization.JSON)
    public sealed interface Events permits Created {
        @PartitioningKey
        Reference<Sale> id();
    }

    public record Created(Reference<Sale> id) implements Events {
    }
}

