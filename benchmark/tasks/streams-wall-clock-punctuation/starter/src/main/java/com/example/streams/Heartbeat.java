package com.example.streams;

import be.appify.prefab.core.domain.Keyed;
import java.time.Instant;

public record Heartbeat(String id, long sequence, Instant emittedAt) implements Keyed<String> {
    @Override
    public String key() {
        return id;
    }
}
