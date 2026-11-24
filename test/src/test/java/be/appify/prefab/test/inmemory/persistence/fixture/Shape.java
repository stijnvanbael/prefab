package be.appify.prefab.test.inmemory.persistence.fixture;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/** Sealed aggregate: all variants are stored through the repository of the root. */
public sealed interface Shape {

    String id();

    record Circle(@Id String id, @Version long version, double radius) implements Shape {
    }

    record Square(@Id String id, @Version long version, double side) implements Shape {
    }
}
