package be.appify.prefab.test.inmemory.persistence.broken;

import org.springframework.data.annotation.Id;

/** Aggregate of a repository with an unsupported derived query. */
public record Gadget(@Id String id, String name) {
}
