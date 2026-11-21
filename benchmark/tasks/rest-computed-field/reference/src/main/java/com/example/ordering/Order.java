package com.example.ordering;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Computed;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        @NotNull String customer,
        @NotNull List<OrderLine> lines
) {
    @Create
    public Order(@NotNull String customer, @NotNull List<OrderLine> lines) {
        this(Reference.create(), 0L, customer, lines);
    }

    @Computed
    public BigDecimal total() {
        return lines.stream()
                .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Computed
    public int lineCount() {
        return lines.size();
    }
}
