package dbmigration.indexed;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.Indexed;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @Filter @NotNull String name,
        @Indexed(unique = true) @Size(max = 100) @NotNull String sku,
        String description
) {
    public Product(@NotNull String name, @NotNull String sku, String description) {
        this(Reference.create(), 0L, name, sku, description);
    }
}

