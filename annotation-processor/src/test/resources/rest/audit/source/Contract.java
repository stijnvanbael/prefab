package rest.audit;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.audit.CreatedAt;
import be.appify.prefab.core.annotations.audit.CreatedBy;
import be.appify.prefab.core.annotations.audit.LastModifiedAt;
import be.appify.prefab.core.annotations.audit.LastModifiedBy;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import java.time.Instant;

import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
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
