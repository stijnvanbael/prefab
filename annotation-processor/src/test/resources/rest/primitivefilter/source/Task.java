package rest.primitivefilter;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Task(
        @Id String id,
        @Version long version,
        String title,
        @Filter boolean done,
        @Filter int priority) {

    @Create
    public Task(String title, boolean done, int priority) {
        this(UUID.randomUUID().toString(), 0L, title, done, priority);
    }
}
