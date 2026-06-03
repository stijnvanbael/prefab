package mother.childwithparent;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.GetById;
import java.util.UUID;

import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Organisation(
        @Id String id,
        @Version long version,
        String name) {

    @be.appify.prefab.core.annotations.rest.Create
    public Organisation(String name) {
        this(UUID.randomUUID().toString(), 0L, name);
    }
}

