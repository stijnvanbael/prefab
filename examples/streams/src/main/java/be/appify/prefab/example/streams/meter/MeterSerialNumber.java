package be.appify.prefab.example.streams.meter;

import be.appify.prefab.core.domain.Key;

public record MeterSerialNumber(String value) implements Key<MeterSerialNumber> {
}
