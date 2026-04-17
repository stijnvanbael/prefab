package dbmigration.varcharsize;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record ProductWithOuterAnnotation(
        @Id String id,
        @Version long version,
        @Size(max = 100) Name name) {

    public record Name(String value) {}
}

