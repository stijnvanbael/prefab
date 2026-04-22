package dbmigration.optout;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration(enabled = false)
public record Product(
        @Id String id,
        @Version long version,
        @NotNull String name) {}

