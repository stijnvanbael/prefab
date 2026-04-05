package mongodb.nestedindex;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Indexed;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import java.math.BigDecimal;

@Aggregate
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @Filter @NotNull String name,
        @NotNull Price price
) {
    public Product(@NotNull String name, @NotNull BigDecimal amount, @NotNull String currency) {
        this(Reference.create(), 0L, name, new Price(amount, currency));
    }

    public record Price(
            @NotNull BigDecimal amount,
            @Indexed @NotNull String currency
    ) {
    }
}
