package rest.plural;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.service.Reference;
import java.util.UUID;

import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Gosling(
        @Id String id,
        @Version long version,
        @Parent Reference<Goose> goose,
        String name
) {
    @Create
    public Gosling(Goose goose, String name) {
        this(UUID.randomUUID().toString(), 0L, new Reference<>(goose.id()), name);
    }
}
