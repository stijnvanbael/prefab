package com.example.shipping;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Shipment(
        @Id Reference<Shipment> id,
        @Version long version,
        @NotNull String recipient,
        @Nullable String carrier,
        @Nullable String trackingNumber
) {
    @Create
    public Shipment(@NotNull String recipient) {
        this(Reference.create(), 0L, recipient, null, null);
    }

    @Update(path = "/dispatch", method = "POST")
    public Shipment dispatch(@NotNull String carrier, @NotNull String trackingNumber) {
        return new Shipment(id, version, recipient, carrier, trackingNumber);
    }
}
