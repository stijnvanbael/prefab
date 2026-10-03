package dbmigration.dbcolumn;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbColumn;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record EmbeddingMissingConverter(
        @Id Reference<EmbeddingMissingConverter> id,
        @Version long version,
        String name,
        @DbColumn(type = "vector(1536)", converterName = "does.not.Exist")
        float[] embedding
) {
}

