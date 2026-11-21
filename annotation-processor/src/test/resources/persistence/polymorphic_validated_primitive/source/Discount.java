package persistence.polymorphic_validated_primitive;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public sealed interface Discount permits Discount.Percentage, Discount.Fixed {

    record Percentage(
            @Id Reference<Discount> id,
            @Version long version,
            @Positive @Max(100) int percentage
    ) implements Discount {
    }

    record Fixed(
            @Id Reference<Discount> id,
            @Version long version,
            @Positive double amount
    ) implements Discount {
    }
}
