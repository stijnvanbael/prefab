package rest.testclient;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
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
public record Person(
        @Id String id,
        @Version long version,
        String name,
        String email) {
    @Create
    public Person(String name, String email) {
        this(UUID.randomUUID().toString(), 0L, name, email);
    }

    @Update
    public Person update(String name, String email) {
        return new Person(id, version, name, email);
    }
}
