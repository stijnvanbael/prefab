package tenant.dbmigration;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.TenantId;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record Project(
        @Id String id,
        @Version long version,
        @TenantId String organisationId,
        String name) {

    public Project(String name) {
        this(UUID.randomUUID().toString(), 0L, null, name);
    }
}
