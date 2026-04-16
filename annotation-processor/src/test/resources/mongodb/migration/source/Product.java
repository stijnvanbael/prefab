package mongodb.migration;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.DbRename;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @DbRename("firstName") @NotNull String givenName,
        @NotNull String description
) {
    public Product(@NotNull String givenName, @NotNull String description) {
        this(Reference.create(), 0L, givenName, description);
    }
}
