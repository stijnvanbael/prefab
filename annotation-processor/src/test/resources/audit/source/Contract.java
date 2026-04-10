package audit;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.audit.CreatedAt;
import be.appify.prefab.core.annotations.audit.CreatedBy;
import be.appify.prefab.core.annotations.audit.LastModifiedAt;
import be.appify.prefab.core.annotations.audit.LastModifiedBy;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
@GetById
@DbMigration
public record Contract(
        @Id String id,
        @Version long version,
        String title,
        @CreatedAt Instant createdAt,
        @CreatedBy String createdBy,
        @LastModifiedAt Instant lastModifiedAt,
        @LastModifiedBy String lastModifiedBy) {

    @Create
    public Contract(String title) {
        this(UUID.randomUUID().toString(), 0L, title, null, null, null, null);
    }

    @Update
    public Contract update(String title) {
        return new Contract(id, version, title, createdAt, createdBy, lastModifiedAt, lastModifiedBy);
    }
}
