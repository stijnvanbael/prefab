package dbmigration.multipackage.product;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @NotNull String name
) {
    public Product(@NotNull String name) {
        this(Reference.create(), 0L, name);
    }
}

