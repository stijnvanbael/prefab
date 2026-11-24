package com.example.streams;

import be.appify.prefab.core.domain.Keyed;

public record Pulse(String id, String value) implements Keyed<String> {
    @Override
    public String key() {
        return id;
    }
}
