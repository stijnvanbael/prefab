package com.example.billing;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.problem.ConflictException;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Invoice(
        @Id Reference<Invoice> id,
        @Version long version,
        @NotNull String customer,
        @NotNull BigDecimal amount,
        @NotNull InvoiceStatus status
) {
    @Create
    public Invoice(@NotNull String customer, @NotNull BigDecimal amount) {
        this(Reference.create(), 0L, customer, amount, InvoiceStatus.OPEN);
    }

    @Update(path = "/pay", method = "POST")
    public Invoice pay() {
        if (status == InvoiceStatus.PAID) {
            throw new ConflictException("Invoice is already paid");
        }
        return new Invoice(id, version, customer, amount, status);
    }
}
