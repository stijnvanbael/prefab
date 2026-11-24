package rest.textfield;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import be.appify.prefab.core.annotations.Text;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Product(
        @Id String id,
        @Version long version,
        String name,
        @Text String description) {
    @Create
    public Product(@NotNull String name, String description) {
        this(UUID.randomUUID().toString(), 0L, name, description);
    }
}
