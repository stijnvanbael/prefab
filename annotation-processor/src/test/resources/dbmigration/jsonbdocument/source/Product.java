package dbmigration.jsonbdocument;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbDocument;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.Indexed;
import be.appify.prefab.core.service.Reference;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @NotNull @Size(max = 255) String name,
        @Indexed ProductDetails details,
        @Nullable List<Tag> tags
) {
    public Product(@NotNull @Size(max = 255) String name, ProductDetails details) {
        this(Reference.create(), 0L, name, details, null);
    }

    public Product(@NotNull @Size(max = 255) String name, ProductDetails details, List<Tag> tags) {
        this(Reference.create(), 0L, name, details, tags);
    }
}
