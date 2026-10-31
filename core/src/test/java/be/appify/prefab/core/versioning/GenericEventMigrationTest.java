package be.appify.prefab.core.versioning;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventMigration;
import be.appify.prefab.core.annotations.Versioned;
import be.appify.prefab.core.domain.EventType;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenericEventMigrationTest {
    @Event(topic = "strings", typeArguments = String.class, version = "2")
    @Event(topic = "integers", typeArguments = Integer.class, version = "3")
    public record Changed<T>(T payload) {}
    public record Old<T>(T value) {}

    @EventMigration(from = "1", to = "2")
    static class Strings implements Migration<Old<String>, Changed<String>> {
        public Changed<String> migrate(Old<String> old) { return new Changed<>(old.value()); }
    }
    @EventMigration(from = "1", to = "3")
    static class Integers implements Migration<Old<Integer>, Changed<Integer>> {
        public Changed<Integer> migrate(Old<Integer> old) { return new Changed<>(old.value() + 1); }
    }

    @Versioned(version = "2")
    public record Address(String street) {}
    public record OldAddress(String line) {}
    @EventMigration(from = "1", to = "2")
    static class Addresses implements Migration<OldAddress, Address> {
        public Address migrate(OldAddress old) { return new Address(old.line()); }
    }
    @Event(topic = "addresses", typeArguments = Address.class)
    public record Envelope<T>(List<T> payload) {}

    @Test
    void independentChainsRetainSnapshotArguments() {
        var mapper = JsonMapper.builder().build();
        var migrator = new EventMigrator(List.of(new Strings(), new Integers()));
        var strings = new EventType<Changed<String>>() {};
        var integers = new EventType<Changed<Integer>>() {};
        assertThat(migrator.plan(strings, MessageVersions.initial())
                .apply(mapper.readTree("{\"value\":\"hello\"}"), mapper)).isEqualTo(new Changed<>("hello"));
        assertThat(migrator.plan(integers, MessageVersions.initial())
                .apply(mapper.readTree("{\"value\":3}"), mapper)).isEqualTo(new Changed<>(4));
        assertThatThrownBy(() -> new EventMigrator(List.of(new Strings()))
                .plan(integers, MessageVersions.initial())).hasMessageContaining("no migration");
    }

    @Test
    void resolvesAndMigratesNestedComponentsThroughConcreteArguments() {
        var mapper = JsonMapper.builder().build();
        var type = new EventType<Envelope<Address>>() {};
        var plan = new EventMigrator(List.of(new Addresses())).plan(type, MessageVersions.initial());
        assertThat(plan.isRequired()).isTrue();
        assertThat(plan.apply(mapper.readTree("{\"payload\":[{\"line\":\"Main\"}]}"), mapper))
                .isEqualTo(new Envelope<>(List.of(new Address("Main"))));
        assertThatThrownBy(() -> EventMigrator.none().plan(type, MessageVersions.initial())
                .apply(mapper.readTree("{\"payload\":[{\"line\":\"Main\"}]}"), mapper))
                .hasMessageContaining("no migration");
    }
}
