package tenant.project;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.TenantId;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
@GetById
public record Project(
        @Id String id,
        @Version long version,
        @TenantId String organisationId,
        String name,
        String description) {

    @Create
    public Project(String name, String description) {
        this(UUID.randomUUID().toString(), 0L, null, name, description);
    }

    @Update
    public Project update(String name, String description) {
        return new Project(id, version, organisationId, name, description);
    }

    @Delete
    public void delete() {
    }
}
