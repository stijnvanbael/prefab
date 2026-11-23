package com.example.watch;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import com.example.catalog.Product;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record PriceWatch(
        @Id Reference<PriceWatch> id,
        @Version long version,
        @NotNull String email,
        @NotNull Reference<Product> product,
        @NotNull BigDecimal currentPrice
) {
    @Create
    public PriceWatch(@NotNull String email, @NotNull Reference<Product> product, @NotNull BigDecimal currentPrice) {
        this(Reference.create(), 0L, email, product, currentPrice);
    }
}
