package tenant.multiple;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.TenantId;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Product(
        @Id String id,
        @Version long version,
        @TenantId String organisationId,
        @TenantId String secondTenantId,
        String name) {

    public Product(String name) {
        this(UUID.randomUUID().toString(), 0L, null, null, name);
    }
}
