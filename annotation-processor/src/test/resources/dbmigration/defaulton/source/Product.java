package dbmigration.defaulton;

import be.appify.prefab.core.annotations.Aggregate;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Product(
        @Id String id,
        @Version long version,
        @NotNull String name) {}

