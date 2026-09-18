package rest.transientfield;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.Transient;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
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
        @Transient String previewToken
) {
    @Create
    public Product(@NotNull String name, @NotNull String previewToken) {
        this(UUID.randomUUID().toString(), 0L, name, previewToken);
    }
}
