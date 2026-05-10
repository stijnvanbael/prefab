package dbmigration.dbcolumn;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbColumn;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record EmbeddingVariants(
        @Id Reference<EmbeddingVariants> id,
        @Version long version,
        @DbColumn(type = "vector(384)")
        Float[] embedding,
        @DbColumn(type = "bytea")
        byte[] digest,
        @DbColumn(type = "jsonb")
        Metadata metadata
) {
    public record Metadata(String source) {
    }
}

