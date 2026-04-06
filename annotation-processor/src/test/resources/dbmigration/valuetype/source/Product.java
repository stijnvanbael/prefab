package dbmigration.valuetype;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record Product(
        @Id String id,
        @Version long version,
        Name name,
        Price price) {

    public record Name(String value) {}

    public record Price(BigDecimal value) {}
}
