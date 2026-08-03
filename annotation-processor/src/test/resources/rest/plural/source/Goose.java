package rest.plural;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.HttpMethod;
import be.appify.prefab.core.annotations.rest.Update;

import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate(plural = "Geese")
@GetList
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Goose(
        @Id String id,
        @Version long version,
        @Filter String name
) {
    @Create(method = HttpMethod.PUT, path = "/{id}")
    public Goose(String id, String name) {
        this(id, 0L, name);
    }

    @Update(method = HttpMethod.PUT)
    public Goose rename(String name) {
        return new Goose(id, version, name);
    }
}
