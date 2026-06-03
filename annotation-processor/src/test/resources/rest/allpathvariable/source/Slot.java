package rest.allpathvariable;

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
public record Slot(
        @Id String id,
        @Version long version,
        String day,
        String hour) {

    @Create(path = "/{day}/{hour}")
    public Slot(String day, String hour) {
        this(UUID.randomUUID().toString(), 0L, day, hour);
    }

    @Update(path = "/{day}/{hour}")
    public Slot reschedule(String day, String hour) {
        return new Slot(id, version, day, hour);
    }
}

