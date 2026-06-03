package mother.invalideexample.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import java.util.UUID;

import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record BadExample(
        @Id String id,
        @Version long version,
        String name) {

    @Create
    public BadExample(@Example("not-a-number") int count) {
        this(UUID.randomUUID().toString(), 0L, String.valueOf(count));
    }
}

