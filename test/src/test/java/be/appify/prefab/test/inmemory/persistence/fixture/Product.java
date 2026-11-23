package be.appify.prefab.test.inmemory.persistence.fixture;

import org.springframework.data.annotation.Id;

/** Aggregate with a searchable name. */
public record Product(@Id String id, String name) {
}
