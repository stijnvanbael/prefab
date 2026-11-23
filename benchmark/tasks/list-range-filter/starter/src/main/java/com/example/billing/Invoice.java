package com.example.billing;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Invoice(
        @Id Reference<Invoice> id,
        @Version long version,
        @NotNull String number,
        @NotNull BigDecimal amount,
        @NotNull LocalDate issuedOn
) {
    @Create
    public Invoice(@NotNull String number, @NotNull BigDecimal amount, @NotNull LocalDate issuedOn) {
        this(Reference.create(), 0L, number, amount, issuedOn);
    }
}
