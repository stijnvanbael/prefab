package com.example.catalog;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @NotNull String name,
        @NotNull BigDecimal price
) {
    private static final EventPublisher<PriceChanged> PRICE_CHANGED = EventPublisher.bind(PriceChanged.class);

    @Create
    public Product(@NotNull String name, @NotNull BigDecimal price) {
        this(Reference.create(), 0L, name, price);
    }

    @Update(path = "/price")
    public Product changePrice(@NotNull BigDecimal price) {
        PRICE_CHANGED.publish(new PriceChanged(id, price));
        return new Product(id, version, name, price);
    }
}
