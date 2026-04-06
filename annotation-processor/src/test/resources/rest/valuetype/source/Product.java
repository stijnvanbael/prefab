package rest.valuetype;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Product(
        @Id String id,
        @Version long version,
        Name name,
        Price price) {

    @Create
    public Product(@NotNull Name name, @NotNull Price price) {
        this(UUID.randomUUID().toString(), 0L, name, price);
    }

    public record Name(String value) {}

    public record Price(BigDecimal value) {}
}
