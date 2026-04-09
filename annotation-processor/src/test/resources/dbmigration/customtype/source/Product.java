package dbmigration.customtype;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.CustomType;
import be.appify.prefab.core.annotations.DbMigration;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record Product(
        @Id String id,
        @Version long version,
        String name,
        Result<String, Integer> result) {

    @CustomType
    public record Result<L, R>(L left, R right) {}
}
