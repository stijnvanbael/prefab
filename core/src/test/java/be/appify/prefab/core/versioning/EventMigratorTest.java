package be.appify.prefab.core.versioning;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventMigration;
import be.appify.prefab.core.annotations.Versioned;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventMigratorTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Event(topic = "users", version = "3.0.0")
    record UserCreated(String fullName, String country) {
    }

    record UserCreatedV1(String firstName, String lastName) {
    }

    record UserCreatedV2(String fullName) {
    }

    @EventMigration(from = "1", to = "2")
    static class V1ToV2 implements Migration<UserCreatedV1, UserCreatedV2> {
        @Override
        public UserCreatedV2 migrate(UserCreatedV1 old) {
            return new UserCreatedV2(old.firstName() + " " + old.lastName());
        }
    }

    @EventMigration(from = "2", to = "3")
    static class V2ToV3 implements Migration<UserCreatedV2, UserCreated> {
        @Override
        public UserCreated migrate(UserCreatedV2 old) {
            return new UserCreated(old.fullName(), "BE");
        }
    }

    @Versioned(version = "2.0.0")
    record Address(String street, String city) {
    }

    record AddressV1(String line) {
    }

    @EventMigration(from = "1", to = "2")
    static class AddressMigration implements Migration<AddressV1, Address> {
        @Override
        public Address migrate(AddressV1 old) {
            var parts = old.line().split(",");
            return new Address(parts[0], parts[1]);
        }
    }

    @Event(topic = "orders")
    record OrderPlaced(
            Address billing,
            List<Address> shipping,
            Optional<Address> pickup,
            Map<String, Address> byName
    ) {
    }

    @Event(topic = "legacy", version = "2.0.0", minSupportedVersion = "2.0.0")
    record Legacy(String value) {
    }

    private EventMigrator migrator() {
        return new EventMigrator(List.of(new V1ToV2(), new V2ToV3(), new AddressMigration()));
    }

    private static MessageVersions versions(String event, String components) {
        return MessageVersions.from(name -> switch (name) {
            case EventVersions.EVENT_VERSION_HEADER -> event;
            case EventVersions.COMPONENT_VERSIONS_HEADER -> components;
            default -> null;
        });
    }

    @Test
    void chainsMigrationsAcrossMajorVersions() {
        var plan = migrator().plan(UserCreated.class, versions("1.4.2", null));

        var result = plan.apply(mapper.readTree("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"), mapper);

        assertThat(plan.isRequired()).isTrue();
        assertThat(result).isEqualTo(new UserCreated("Ada Lovelace", "BE"));
    }

    @Test
    void startsTheChainAtTheMajorVersionOfTheMessage() {
        var plan = migrator().plan(UserCreated.class, versions("2.0.0", null));

        var result = plan.apply(mapper.readTree("{\"fullName\":\"Ada Lovelace\"}"), mapper);

        assertThat(result).isEqualTo(new UserCreated("Ada Lovelace", "BE"));
    }

    @Test
    void readsMessagesOfTheCurrentMajorDirectly() {
        assertThat(migrator().plan(UserCreated.class, versions("3.0.0", null)).isRequired()).isFalse();
        assertThat(migrator().plan(UserCreated.class, versions("3.7.1", null)).isRequired()).isFalse();
    }

    @Test
    void readsMessagesWithoutVersionAsInitialVersion() {
        assertThat(EventMigrator.none().plan(OrderPlaced.class, MessageVersions.initial()).isRequired()).isFalse();
        assertThat(MessageVersions.initial().event()).isEqualTo(new EventVersion(1, 0, 0));
    }

    @Test
    void rejectsMessagesFromAHigherMajorVersion() {
        assertThatThrownBy(() -> migrator().plan(UserCreated.class, versions("4.0.0", null)))
                .isInstanceOf(UnsupportedEventVersionException.class)
                .hasMessageContaining("4.0.0")
                .hasMessageContaining("newer");
    }

    @Test
    void rejectsMajorVersionsWithoutRemainingChain() {
        var withoutV1 = new EventMigrator(List.of(new V2ToV3()));

        assertThatThrownBy(() -> withoutV1.plan(UserCreated.class, versions("1.0.0", null)))
                .isInstanceOf(UnsupportedEventVersionException.class)
                .hasMessageContaining("no migration from major version 1");
        assertThat(withoutV1.plan(UserCreated.class, versions("2.0.0", null)).isRequired()).isTrue();
    }

    @Test
    void rejectsVersionsBelowTheMinimumSupportedVersion() {
        assertThatThrownBy(() -> EventMigrator.none().plan(Legacy.class, versions("1.0.0", null)))
                .isInstanceOf(UnsupportedEventVersionException.class)
                .hasMessageContaining("minimum supported version 2.0.0");
    }

    @Test
    void rejectsMalformedVersionHeaders() {
        assertThatThrownBy(() -> versions("abc", null)).isInstanceOf(UnsupportedEventVersionException.class);
    }

    @Test
    void rejectsChainsThatDoNotEndAtTheCurrentMajorVersion() {
        assertThatThrownBy(() -> new EventMigrator(List.of(new V1ToV2())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNonContiguousChains() {
        @EventMigration(from = "3", to = "4")
        class Gap implements Migration<UserCreatedV2, UserCreated> {
            @Override
            public UserCreated migrate(UserCreatedV2 old) {
                return null;
            }
        }
        assertThatThrownBy(() -> new EventMigrator(List.of(new V1ToV2(), new Gap())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not contiguous");
    }

    @Test
    void migratesNestedComponentsAtAnyDepthBeforeTheEvent() {
        var payload = mapper.readTree("""
                {"billing":{"line":"Main St,Ghent"},
                 "shipping":[{"line":"A,B"},{"line":"C,D"}],
                 "pickup":{"line":"E,F"},
                 "byName":{"home":{"line":"G,H"}}}""");

        var result = migrator().plan(OrderPlaced.class, versions("1.0.0", Address.class.getName() + "=1.0.0"))
                .apply(payload, mapper);

        assertThat(result).isEqualTo(new OrderPlaced(
                new Address("Main St", "Ghent"),
                List.of(new Address("A", "B"), new Address("C", "D")),
                Optional.of(new Address("E", "F")),
                Map.of("home", new Address("G", "H"))));
    }

    @Test
    void leavesCurrentComponentsUntouched() {
        var plan = migrator().plan(OrderPlaced.class, versions("1.0.0", Address.class.getName() + "=2.0.0"));

        assertThat(plan.isRequired()).isFalse();
    }

    @Test
    void migratesAvroRecordsThroughTheirTree() {
        var schema = new Schema.Parser().parse("""
                {"type":"record","name":"UserCreated","fields":[
                  {"name":"firstName","type":"string"},{"name":"lastName","type":["null","string"]}]}""");
        var record = new GenericData.Record(schema);
        record.put("firstName", "Ada");
        record.put("lastName", "Lovelace");

        var result = migrator().plan(UserCreated.class, versions("1.0.0", null))
                .apply(AvroTrees.toTree(record, mapper), mapper);

        assertThat(result).isEqualTo(new UserCreated("Ada Lovelace", "BE"));
    }

    @Test
    void registersPreviousTypeNames() {
        @EventMigration(from = "2", to = "3", fromType = "OldUserCreated")
        class Renamed implements Migration<UserCreatedV2, UserCreated> {
            @Override
            public UserCreated migrate(UserCreatedV2 old) {
                return null;
            }
        }

        assertThat(new EventMigrator(List.of(new Renamed())).typeAliases())
                .containsEntry("OldUserCreated", UserCreated.class);
    }

    @Test
    void migrationsCanBeUnitTestedWithoutABroker() {
        assertThat(new V1ToV2().migrate(new UserCreatedV1("Ada", "Lovelace"))).isEqualTo(new UserCreatedV2("Ada Lovelace"));
    }

    @Test
    void stampsVersionHeadersIncludingComponentVersions() {
        var event = new OrderPlaced(new Address("a", "b"), List.of(), Optional.empty(), Map.of());

        assertThat(EventVersions.headersFor(event))
                .containsEntry(EventVersions.EVENT_VERSION_HEADER, "1.0.0")
                .containsEntry(EventVersions.COMPONENT_VERSIONS_HEADER, Address.class.getName() + "=2.0.0");
        assertThat(EventVersions.headersFor(new UserCreated("a", "b")))
                .containsEntry(EventVersions.EVENT_VERSION_HEADER, "3.0.0")
                .doesNotContainKey(EventVersions.COMPONENT_VERSIONS_HEADER);
        assertThat(EventVersions.headersFor("not an event")).isEmpty();
    }
}
