package rest.audit;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.audit.CreatedAt;
import be.appify.prefab.core.annotations.audit.CreatedBy;
import be.appify.prefab.core.annotations.audit.LastModifiedAt;
import be.appify.prefab.core.annotations.audit.LastModifiedBy;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Contract(
        @Id Reference<Contract> id,
        @Version long version,
        String title,
        @CreatedAt Instant createdAt,
        @CreatedBy String createdBy,
        @LastModifiedAt Instant lastModifiedAt,
        @LastModifiedBy String lastModifiedBy
) {
    @Create
    public Contract(String title) {
        this(Reference.create(), 0L, title, null, null, null, null);
    }

    @Update
    public Contract update(String title) {
        return new Contract(id, version, title, createdAt, createdBy, lastModifiedAt, lastModifiedBy);
    }
}
