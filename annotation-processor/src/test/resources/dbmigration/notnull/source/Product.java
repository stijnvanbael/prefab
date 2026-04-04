package dbmigration.notnull;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import jakarta.annotation.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record Product(
        @Id String id,
        @Version long version,
        String name,
        @Nullable String description) {}
