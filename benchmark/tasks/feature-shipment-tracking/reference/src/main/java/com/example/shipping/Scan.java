package com.example.shipping;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
public record Scan(
        @Id Reference<Scan> id,
        @Version long version,
        @Parent @NotNull Reference<Shipment> shipment,
        @NotBlank String location,
        @NotNull ScanType type
) {
    private static final EventPublisher<ScanRecorded> RECORDED = EventPublisher.bind(ScanRecorded.class);

    @Create
    public Scan(@NotNull Reference<Shipment> shipment, @NotBlank String location, @NotNull ScanType type) {
        this(Reference.create(), 0L, shipment, location, type);
        RECORDED.publish(new ScanRecorded(shipment, location, type));
    }
}
