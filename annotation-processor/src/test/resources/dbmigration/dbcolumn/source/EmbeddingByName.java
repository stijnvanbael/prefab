package dbmigration.dbcolumn;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbColumn;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record EmbeddingByName(
        @Id Reference<EmbeddingByName> id,
        @Version long version,
        String name,
        @DbColumn(type = "vector(1536)", converterName = "dbmigration.dbcolumn.FloatArrayToVectorConverter")
        float[] embedding
) {
}

