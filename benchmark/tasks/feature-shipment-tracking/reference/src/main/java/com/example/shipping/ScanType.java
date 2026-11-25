package com.example.shipping;

public enum ScanType {
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED;

    ShipmentStatus status() {
        return ShipmentStatus.valueOf(name());
    }
}
