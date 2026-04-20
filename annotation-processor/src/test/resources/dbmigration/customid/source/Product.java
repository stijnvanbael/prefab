package dbmigration.customid;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record Product(
        @Id String productId,
        @Version long version,
        String name) {}

