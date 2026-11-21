package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.streams.kafka.KafkaPrefabStreams.keyTypeOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyTypeResolutionTest {

    // ── test-fixture types ─────────────────────────────────────────────────────

    record FooId(String value) {}

    /** Case 1: direct Keyed implementation on the concrete class. */
    record DirectEntity(FooId id) implements Keyed<FooId> {
        @Override
        public FooId key() { return id; }
    }

    /** Case 2: Keyed implemented on a superclass. */
    abstract static class BaseEntity implements Keyed<FooId> {}

    static class SubclassEntity extends BaseEntity {
        @Override
        public FooId key() { return new FooId("x"); }
    }

    /** Case 3: intermediate non-generic interface that extends Keyed<FooId>. */
    interface NonGenericBridge extends Keyed<FooId> {}

    record BridgeEntity(FooId id) implements NonGenericBridge {
        @Override
        public FooId key() { return id; }
    }

    /** Case 4: intermediate generic interface — type parameter resolved at the concrete class. */
    interface GenericBridge<K> extends Keyed<K> {}

    record GenericBridgeEntity(FooId id) implements GenericBridge<FooId> {
        @Override
        public FooId key() { return id; }
    }

    /** Case 5: deep interface chain (interface extends interface extends Keyed). */
    interface Level1Bridge extends NonGenericBridge {}

    record DeepBridgeEntity(FooId id) implements Level1Bridge {
        @Override
        public FooId key() { return id; }
    }

    /** Case 6: no Keyed anywhere — must throw. */
    record Unkeyed(String value) {}

    // ── tests ──────────────────────────────────────────────────────────────────

    @Test
    void keyTypeOf_directImplementation_resolvesKeyType() {
        assertThat(keyTypeOf(DirectEntity.class)).isEqualTo(FooId.class);
    }

    @Test
    void keyTypeOf_keyedOnSuperclass_resolvesKeyType() {
        assertThat(keyTypeOf(SubclassEntity.class)).isEqualTo(FooId.class);
    }

    @Test
    void keyTypeOf_nonGenericBridgeInterface_resolvesKeyType() {
        assertThat(keyTypeOf(BridgeEntity.class)).isEqualTo(FooId.class);
    }

    @Test
    void keyTypeOf_genericBridgeInterface_resolvesKeyType() {
        assertThat(keyTypeOf(GenericBridgeEntity.class)).isEqualTo(FooId.class);
    }

    @Test
    void keyTypeOf_deepInterfaceChain_resolvesKeyType() {
        assertThat(keyTypeOf(DeepBridgeEntity.class)).isEqualTo(FooId.class);
    }

    @Test
    void keyTypeOf_noKeyedInHierarchy_throwsIllegalArgumentException() {
        @SuppressWarnings("unchecked")
        var unkeyedClass = (Class<Keyed<FooId>>) (Class<?>) Unkeyed.class;
        assertThatThrownBy(() -> keyTypeOf(unkeyedClass))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Unkeyed.class.getName());
    }
}


