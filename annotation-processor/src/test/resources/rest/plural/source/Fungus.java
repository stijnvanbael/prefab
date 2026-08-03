package rest.plural;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetList;
import java.util.UUID;

import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate(plural = "Fungi")
@GetList
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public sealed interface Fungus permits Fungus.Mold, Fungus.Yeast {

    String id();

    long version();

    record Mold(@Id String id, @Version long version, double sporeCount) implements Fungus {
        @Create
        public Mold(double sporeCount) {
            this(UUID.randomUUID().toString(), 0L, sporeCount);
        }
    }

    record Yeast(@Id String id, @Version long version, String strain) implements Fungus {
        @Create
        public Yeast(String strain) {
            this(UUID.randomUUID().toString(), 0L, strain);
        }
    }
}
