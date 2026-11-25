package com.example.shipping;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Shipment(
        @Id Reference<Shipment> id,
        @Version long version,
        @NotBlank String orderNumber,
        @NotBlank String address,
        @Filter ShipmentStatus status,
        int scanCount,
        @Nullable String lastLocation
) {
    private static final EventPublisher<ShipmentDelivered> DELIVERED = EventPublisher.bind(ShipmentDelivered.class);

    @Create
    public Shipment(@NotBlank String orderNumber, @NotBlank String address) {
        this(Reference.create(), 0L, orderNumber, address, ShipmentStatus.CREATED, 0, null);
    }

    @EventHandler
    @ByReference(property = "shipment")
    public Shipment onScanRecorded(ScanRecorded scan) {
        var next = status == ShipmentStatus.DELIVERED ? status : scan.type().status();
        if (next == ShipmentStatus.DELIVERED && status != ShipmentStatus.DELIVERED) {
            DELIVERED.publish(new ShipmentDelivered(id, orderNumber, address));
        }
        return new Shipment(id, version, orderNumber, address, next, scanCount + 1, scan.location());
    }
}
