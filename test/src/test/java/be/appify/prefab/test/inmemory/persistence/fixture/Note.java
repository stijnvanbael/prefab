package be.appify.prefab.test.inmemory.persistence.fixture;

import org.springframework.data.annotation.Id;

/** Aggregate without a version. */
public record Note(@Id String id, String text) {
}
