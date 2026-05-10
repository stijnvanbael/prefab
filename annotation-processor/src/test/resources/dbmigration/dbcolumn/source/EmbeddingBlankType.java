package dbmigration.dbcolumn;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbColumn;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record EmbeddingBlankType(
        @Id Reference<EmbeddingBlankType> id,
        @Version long version,
        @DbColumn(type = "")
        float[] embedding
) {
}

