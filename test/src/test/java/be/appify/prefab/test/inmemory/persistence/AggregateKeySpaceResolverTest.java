package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.test.inmemory.persistence.fixture.Note;
import be.appify.prefab.test.inmemory.persistence.fixture.Shape;
import be.appify.prefab.test.inmemory.persistence.fixture.Shape.Circle;
import be.appify.prefab.test.inmemory.persistence.fixture.Shape.Square;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AggregateKeySpaceResolverTest {
    private final AggregateKeySpaceResolver resolver = new AggregateKeySpaceResolver();

    @Test
    void resolveKeySpace_givenPlainAggregate_usesItsOwnType() {
        assertThat(resolver.resolveKeySpace(Note.class)).isEqualTo(Note.class.getName());
    }

    @Test
    void resolveKeySpace_givenSealedVariants_usesTheSealedRoot() {
        assertThat(resolver.resolveKeySpace(Circle.class)).isEqualTo(Shape.class.getName());
        assertThat(resolver.resolveKeySpace(Square.class)).isEqualTo(Shape.class.getName());
        assertThat(resolver.resolveKeySpace(Shape.class)).isEqualTo(Shape.class.getName());
    }
}
