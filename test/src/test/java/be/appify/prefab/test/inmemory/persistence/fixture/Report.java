package be.appify.prefab.test.inmemory.persistence.fixture;

import org.springframework.data.annotation.Id;

/** Aggregate of a repository with MongoDB queries. */
public record Report(@Id String id, String title) {
}
